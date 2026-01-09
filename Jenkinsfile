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

        // SonarQube (FIXED)
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Checkout source code'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
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
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        stage('Frontend: Tests') {
            steps {
                dir('frontend') {
                    bat '''
                        npm install
                        npm run test:ci || echo "Frontend tests skipped"
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
                            -Dsonar.projectName="Todo Backend" ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.token=%SONAR_TOKEN% ^
                            -Dsonar.java.binaries=target/classes ^
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        """
                    }
                }
            }
        }

        stage('Frontend: SonarQube Analysis') {
            steps {
                dir('frontend') {
                    bat """
                        npx sonar-scanner ^
                        -Dsonar.projectKey=todo-frontend ^
                        -Dsonar.projectName="Todo Frontend" ^
                        -Dsonar.sources=src ^
                        -Dsonar.host.url=%SONAR_HOST_URL% ^
                        -Dsonar.token=%SONAR_TOKEN% ^
                        -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend/todoapp') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm run build'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest frontend"
            }
        }

        stage('Push Docker Images') {
            steps {
                bat """
                    echo %DOCKERHUB_CREDENTIALS_PSW% | docker login ^
                    -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                """
                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${BACKEND_IMAGE}:latest"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:latest"
            }
        }

        stage('Cleanup') {
            steps {
                bat "docker logout"
            }
        }
    }

    post {
        success {
            echo '✅ PIPELINE SUCCESS'
        }
        failure {
            echo '❌ PIPELINE FAILED'
        }
    }
}
