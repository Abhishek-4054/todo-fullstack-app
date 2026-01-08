pipeline {
    agent any
    
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
        nodejs 'NodeJS-18'
    }
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = 'abhishekc4054'
        BACKEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-frontend"
        IMAGE_TAG = "${BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
            }
        }
        
        stage('Build Backend') {
            steps {
                echo 'Building Spring Boot Backend...'
                dir('backend/todoapp') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                echo 'Building React Frontend...'
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                echo 'Building Docker images...'
                script {
                    bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest backend/todoapp"
                    bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest frontend"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo 'Logging into Docker Hub...'
                script {
                    bat """
                        echo %DOCKERHUB_CREDENTIALS_PSW% | docker login -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                    """
                }
                echo 'Pushing images to Docker Hub...'
                script {
                    bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                    bat "docker push ${BACKEND_IMAGE}:latest"
                    bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    bat "docker push ${FRONTEND_IMAGE}:latest"
                }
            }
        }
        
        stage('Cleanup Local Images') {
            steps {
                echo 'Cleaning up local images...'
                script {
                    bat """
                        docker rmi ${BACKEND_IMAGE}:${IMAGE_TAG} || exit 0
                        docker rmi ${FRONTEND_IMAGE}:${IMAGE_TAG} || exit 0
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline completed successfully!'
            echo "Backend Image: ${BACKEND_IMAGE}:${IMAGE_TAG}"
            echo "Frontend Image: ${FRONTEND_IMAGE}:${IMAGE_TAG}"
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        always {
            echo 'Logging out from Docker Hub...'
            bat 'docker logout || exit 0'
        }
    }
}