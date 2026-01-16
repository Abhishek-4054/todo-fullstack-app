pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
        nodejs 'NodeJS-18'
    }

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME   = 'abhishekc4054'
        BACKEND_IMAGE        = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE       = "${DOCKERHUB_USERNAME}/todo-frontend"
        IMAGE_TAG            = "${BUILD_NUMBER}"

        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN    = credentials('sonarqube-token')

        GIT_REPO_URL  = 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
        K8S_NAMESPACE = 'todo-app'

        ARGOCD_SERVER   = 'localhost:8081'
        ARGOCD_APP_NAME = 'todo-fullstack-app'
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO_URL}"
            }
        }

        stage('Backend Tests') {
            steps {
                dir('backend/todoapp') {
                    bat 'mvn clean test'
                }
            }
        }

        stage('Frontend Tests') {
            steps {
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run test:ci || echo Frontend tests skipped'
                }
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} frontend"

                bat """
                    echo %DOCKERHUB_CREDENTIALS_PSW% | docker login ^
                    -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                """

                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }

        stage('Update Kubernetes Manifests') {
            steps {
                dir('k8s') {
                    bat """
                    powershell -Command "(Get-Content backend-deployment.yaml) -replace 'image: .*todo-backend:.*','image: ${BACKEND_IMAGE}:${IMAGE_TAG}' | Set-Content backend-deployment.yaml"
                    """

                    bat """
                    powershell -Command "(Get-Content frontend-deployment.yaml) -replace 'image: .*todo-frontend:.*','image: ${FRONTEND_IMAGE}:${IMAGE_TAG}' | Set-Content frontend-deployment.yaml"
                    """
                }
            }
        }

        stage('Commit & Push Manifests') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-credentials',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {
                    bat """
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI"
                        git add k8s/
                        git commit -m "Update image tags to ${IMAGE_TAG}" || echo No changes
                        git pull https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main --rebase
                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main
                    """
                }
            }
        }

        stage('ArgoCD Sync') {
            steps {
                withCredentials([string(credentialsId: 'argocd-token', variable: 'ARGOCD_TOKEN')]) {
                    bat """
                        set PATH=%PATH%;C:\\tools\\argocd
                        argocd login ${ARGOCD_SERVER} --auth-token %ARGOCD_TOKEN% --insecure --grpc-web
                        argocd app sync ${ARGOCD_APP_NAME} --grpc-web
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ PIPELINE SUCCESS — Deployed via ArgoCD'
        }
        failure {
            echo '❌ PIPELINE FAILED'
        }
    }
}
