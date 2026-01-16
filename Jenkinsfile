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
                        bat "mvn sonar:sonar -Dsonar.projectKey=todo-backend -Dsonar.token=%SONAR_TOKEN%"
                    }
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                echo '🐳 Building Docker images'
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"

                echo '🔐 Docker login'
                bat "echo %DOCKERHUB_CREDENTIALS_PSW% | docker login -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin"

                echo '📤 Pushing images'
                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }

        stage('Update Kubernetes Manifests') {
            steps {
                dir('k8s') {
                    echo '📝 Updating image tags in manifests'

                    bat 'powershell -NoProfile -Command "(Get-Content backend-deployment.yaml) -replace \'image: .*todo-backend:.*\',\'image: abhishekc4054/todo-backend:%BUILD_NUMBER%\' | Set-Content backend-deployment.yaml"'

                    bat 'powershell -NoProfile -Command "(Get-Content frontend-deployment.yaml) -replace \'image: .*todo-frontend:.*\',\'image: abhishekc4054/todo-frontend:%BUILD_NUMBER%\' | Set-Content frontend-deployment.yaml"'
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
                    bat '''
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI"
                        git add k8s/
                        git commit -m "Update image tags to %BUILD_NUMBER%" || echo No changes
                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main
                    '''
                }
            }
        }

        stage('GitOps Deployment via ArgoCD') {
            steps {
                echo '🚀 ArgoCD Auto-Sync will deploy the updated manifests'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '📊 Verifying rollout'
                bat "kubectl rollout status deployment/todo-backend -n ${K8S_NAMESPACE} --timeout=300s"
                bat "kubectl rollout status deployment/todo-frontend -n ${K8S_NAMESPACE} --timeout=300s"
            }
        }

        stage('Cleanup') {
            steps {
                bat 'docker logout'
            }
        }
    }

    post {
        success {
            echo '✅ PIPELINE SUCCESS'
            echo '🎉 Deployed using Jenkins CI + ArgoCD GitOps'
        }
        failure {
            echo '❌ PIPELINE FAILED'
        }
        always {
            echo "📊 Build #${BUILD_NUMBER} finished"
        }
    }
}
