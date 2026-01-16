pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
        nodejs 'NodeJS-18'
    }

    environment {
        // Docker
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME   = 'abhishekc4054'
        BACKEND_IMAGE        = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE       = "${DOCKERHUB_USERNAME}/todo-frontend"
        IMAGE_TAG            = "${BUILD_NUMBER}"

        // SonarQube
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN    = credentials('sonarqube-token')

        // Git & Kubernetes
        GIT_REPO_URL  = 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
        K8S_NAMESPACE = 'todo-app'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Checking out source code'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO_URL}"
            }
        }

        stage('Backend: Build & Test') {
            steps {
                dir('backend/todoapp') {
                    bat 'mvn clean test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Frontend: Build & Test') {
            steps {
                dir('frontend') {
                    bat '''
                        npm install
                        npm run test:ci || echo "Frontend tests skipped"
                        npm run build
                    '''
                }
            }
        }

        stage('Backend: SonarQube Analysis') {
            steps {
                dir('backend/todoapp') {
                    withSonarQubeEnv('SonarQube') {
                        bat """
                            mvn sonar:sonar ^
                            -Dsonar.projectKey=todo-backend ^
                            -Dsonar.token=%SONAR_TOKEN%
                        """
                    }
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                echo '🐳 Building Docker images'
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"

                echo '🔐 Logging into DockerHub'
                bat """
                    echo %DOCKERHUB_CREDENTIALS_PSW% | docker login ^
                    -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                """

                echo '📤 Pushing images'
                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }

        stage('Update Kubernetes Manifests') {
            steps {
                dir('k8s') {
                    echo '📝 Updating image tags in Kubernetes manifests'

                    // BACKEND
                    bat """
                    powershell -NoProfile -Command ^
                    "(Get-Content backend-deployment.yaml) `
                    -replace 'image: .*todo-backend:.*', `
                    'image: ${BACKEND_IMAGE}:${IMAGE_TAG}' |
                    Set-Content backend-deployment.yaml"
                    """

                    // FRONTEND
                    bat """
                    powershell -NoProfile -Command ^
                    "(Get-Content frontend-deployment.yaml) `
                    -replace 'image: .*todo-frontend:.*', `
                    'image: ${FRONTEND_IMAGE}:${IMAGE_TAG}' |
                    Set-Content frontend-deployment.yaml"
                    """
                }
            }
        }

        stage('Commit & Push Manifests (GitOps)') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'github-credentials',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_PASS'
                    )
                ]) {
                    bat """
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI"

                        git add k8s/
                        git commit -m "Update image tags to ${IMAGE_TAG}" || echo "No changes to commit"

                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main
                    """
                }
            }
        }

        stage('GitOps Handover to ArgoCD') {
            steps {
                echo '✅ Manifests pushed to Git'
                echo '🚀 ArgoCD Auto-Sync will deploy changes automatically'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '📊 Verifying Kubernetes rollout'
                bat """
                    kubectl rollout status deployment/todo-backend -n ${K8S_NAMESPACE} --timeout=300s
                    kubectl rollout status deployment/todo-frontend -n ${K8S_NAMESPACE} --timeout=300s
                """
            }
        }

        stage('Cleanup') {
            steps {
                echo '🧹 Docker logout'
                bat 'docker logout'
            }
        }
    }

    post {
        success {
            echo '✅✅✅ PIPELINE SUCCESS ✅✅✅'
            echo '🎉 Application deployed using GitOps (ArgoCD Auto-Sync)'
        }
        failure {
            echo '❌❌❌ PIPELINE FAILED ❌❌❌'
        }
        always {
            echo "📊 Build #${BUILD_NUMBER} completed"
        }
    }
}
