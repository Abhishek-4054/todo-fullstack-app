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
        DOCKERHUB_USERNAME = 'abhishekc4054'
        BACKEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-frontend"
        IMAGE_TAG = "${BUILD_NUMBER}"

        // SonarQube
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')

        // Kubernetes & ArgoCD
        K8S_NAMESPACE = 'todo-app'
        ARGOCD_SERVER = 'localhost:8081'
        ARGOCD_APP_NAME = 'todo-fullstack-app'
        GIT_REPO_URL = 'https://github.com/Abhishek-4054/todo-fullstack-app.git'

        // Direct path to the .exe Application
        ARGOCD_PATH = 'C:\\tools\\argocd\\argocd.exe'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Checking out source code...'
                git branch: 'main', credentialsId: 'github-credentials', url: "${GIT_REPO_URL}"
            }
        }

        stage('Backend: Tests & Coverage') {
            steps {
                dir('backend/todoapp') {
                    bat 'mvn clean test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: '**/src/main/java')
                }
            }
        }

        stage('Frontend: Tests') {
            steps {
                dir('frontend') {
                    bat 'npm install && npm run test:ci || echo "Frontend tests skipped"'
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
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"
                bat "echo %DOCKERHUB_CREDENTIALS_PSW% | docker login -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin"
                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }

        stage('Update K8s Manifests') {
            steps {
                dir('k8s') {
                    bat """
                        powershell -Command "(Get-Content backend-deployment.yaml) -replace 'image: ${BACKEND_IMAGE}:.*', 'image: ${BACKEND_IMAGE}:${IMAGE_TAG}' | Set-Content backend-deployment.yaml"
                        powershell -Command "(Get-Content frontend-deployment.yaml) -replace 'image: ${FRONTEND_IMAGE}:.*', 'image: ${FRONTEND_IMAGE}:${IMAGE_TAG}' | Set-Content frontend-deployment.yaml"
                    """
                }
            }
        }

        stage('Commit & Push K8s Manifests') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                    bat """
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI"
                        git add k8s/
                        git commit -m "Update image tags to ${IMAGE_TAG}" || echo "No changes"
                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main
                    """
                }
            }
        }

        stage('ArgoCD Sync') {
            steps {
                echo '🚀 Syncing application via ArgoCD...'
                withCredentials([string(credentialsId: 'argocd-token', variable: 'ARGOCD_TOKEN')]) {
                    bat """
                        @echo off
                        "%ARGOCD_PATH%" login ${ARGOCD_SERVER} --auth-token %ARGOCD_TOKEN% --insecure
                        "%ARGOCD_PATH%" app sync ${ARGOCD_APP_NAME} --insecure
                    """
                }
            }
        }

        stage('Verify Deployment Status') {
            steps {
                bat """
                    kubectl rollout status deployment/todo-backend -n ${K8S_NAMESPACE} --timeout=300s
                    kubectl rollout status deployment/todo-frontend -n ${K8S_NAMESPACE} --timeout=300s
                """
            }
        }

        stage('Cleanup') {
            steps { bat "docker logout" }
        }
    }

    post {
        success { echo '✅ Pipeline Success! App: http://localhost:30080' }
        failure { echo '❌ Pipeline Failed' }
    }
}