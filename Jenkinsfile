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

        BACKEND_IMAGE  = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-frontend"

        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout Code') {
            steps {
                echo '📥 Checking out source code'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
            }
        }

        stage('Build Backend') {
            steps {
                echo '🔨 Building Backend'
                dir('backend') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                echo '🔨 Building Frontend'
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo '🐳 Building & pushing Docker images'
                bat """
                docker login -u %DOCKERHUB_USERNAME% -p %DOCKERHUB_CREDENTIALS_PSW%
                docker build -t %BACKEND_IMAGE%:%IMAGE_TAG% backend
                docker build -t %FRONTEND_IMAGE%:%IMAGE_TAG% frontend
                docker push %BACKEND_IMAGE%:%IMAGE_TAG%
                docker push %FRONTEND_IMAGE%:%IMAGE_TAG%
                """
            }
        }

        stage('Update Kubernetes Manifests') {
            steps {
                echo '✏️ Updating Kubernetes YAML image tags'
                dir('k8s') {
                    bat '''
powershell -NoProfile -Command ^
"$content = Get-Content backend-deployment.yaml; ^
 $content = $content -replace 'image:.*todo-backend:.*', 'image: abhishekc4054/todo-backend:${env:BUILD_NUMBER}'; ^
 Set-Content backend-deployment.yaml $content"

powershell -NoProfile -Command ^
"$content = Get-Content frontend-deployment.yaml; ^
 $content = $content -replace 'image:.*todo-frontend:.*', 'image: abhishekc4054/todo-frontend:${env:BUILD_NUMBER}'; ^
 Set-Content frontend-deployment.yaml $content"
'''
                }
            }
        }

        stage('Commit & Push Manifests') {
            steps {
                echo '📤 Pushing updated manifests to Git'
                bat '''
git config user.email "jenkins@local"
git config user.name "jenkins"
git add k8s
git commit -m "Update image tags to build ${BUILD_NUMBER}"
git push origin main
'''
            }
        }
    }

    post {
        success {
            echo '✅ CI completed successfully. ArgoCD will auto-sync 🚀'
        }
        failure {
            echo '❌ CI failed. Check logs.'
        }
    }
}
