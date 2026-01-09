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
        SONAR_HOST_URL = 'http://host.docker.internal:9000'
        SONAR_TOKEN = credentials('sonarqube-token')
        
        // Quality Gates
        CODE_COVERAGE_THRESHOLD = '80'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '📥 Checking out code from GitHub...'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
            }
        }
        
        stage('Backend: Unit Tests') {
            steps {
                echo '🧪 Running backend unit tests...'
                dir('backend/todoapp') {
                    bat '''
                        mvn clean test ^
                        -Dtest=*Test ^
                        -DfailIfNoTests=false
                    '''
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/*Application.class'
                    )
                }
            }
        }
        
        stage('Frontend: Unit Tests') {
            steps {
                echo '🧪 Running frontend unit tests...'
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run test:ci'
                }
            }
            post {
                always {
                    publishHTML([
                        reportDir: 'frontend/coverage',
                        reportFiles: 'index.html',
                        reportName: 'Frontend Coverage Report',
                        allowMissing: true
                    ])
                }
            }
        }
        
        stage('Backend: SonarQube Analysis') {
            steps {
                echo '📊 Running SonarQube analysis for backend...'
                dir('backend/todoapp') {
                    withSonarQubeEnv('SonarQube') {
                        bat """
                            mvn sonar:sonar ^
                            -Dsonar.projectKey=todo-backend ^
                            -Dsonar.projectName="Todo Backend" ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.login=%SONAR_TOKEN% ^
                            -Dsonar.java.binaries=target/classes ^
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        """
                    }
                }
            }
        }
        
        stage('Frontend: SonarQube Analysis') {
            steps {
                echo '📊 Running SonarQube analysis for frontend...'
                dir('frontend') {
                    withSonarQubeEnv('SonarQube') {
                        bat """
                            npx sonar-scanner ^
                            -Dsonar.projectKey=todo-frontend ^
                            -Dsonar.projectName="Todo Frontend" ^
                            -Dsonar.sources=src ^
                            -Dsonar.host.url=%SONAR_HOST_URL% ^
                            -Dsonar.login=%SONAR_TOKEN% ^
                            -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info ^
                            -Dsonar.testExecutionReportPaths=test-report.xml
                        """
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '🚦 Waiting for SonarQube Quality Gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        
        stage('Build Backend') {
            steps {
                echo '🔨 Building Spring Boot Backend...'
                dir('backend/todoapp') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                echo '🔨 Building React Frontend...'
                dir('frontend') {
                    bat 'npm run build'
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                script {
                    bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest backend/todoapp"
                    bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest frontend"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                echo '📤 Pushing images to Docker Hub...'
                script {
                    bat """
                        echo %DOCKERHUB_CREDENTIALS_PSW% | docker login -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                    """
                    bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                    bat "docker push ${BACKEND_IMAGE}:latest"
                    bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    bat "docker push ${FRONTEND_IMAGE}:latest"
                }
            }
        }
        
        stage('Cleanup') {
            steps {
                echo '🧹 Cleaning up local images...'
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
            echo "SonarQube: ${SONAR_HOST_URL}"
            
            emailext(
                subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2>Build Successful</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                    <p><strong>Backend Image:</strong> ${BACKEND_IMAGE}:${IMAGE_TAG}</p>
                    <p><strong>Frontend Image:</strong> ${FRONTEND_IMAGE}:${IMAGE_TAG}</p>
                    <p><strong>SonarQube:</strong> <a href="${SONAR_HOST_URL}">View Report</a></p>
                    <p><strong>Console Output:</strong> <a href="${BUILD_URL}console">${BUILD_URL}console</a></p>
                """,
                to: 'abhishek184057@gmail.com',
                mimeType: 'text/html'
            )
        }
        
        failure {
            echo '❌ Pipeline failed!'
            
            emailext(
                subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2 style="color: red;">Build Failed</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                    <p><strong>Failed Stage:</strong> ${env.STAGE_NAME}</p>
                    <p><strong>Console Output:</strong> <a href="${BUILD_URL}console">${BUILD_URL}console</a></p>
                """,
                to: 'abhishek184057@gmail.com',
                mimeType: 'text/html'
            )
        }
        
        always {
            echo '🔒 Logging out from Docker Hub...'
            bat 'docker logout || exit 0'
            
            // Archive artifacts
            archiveArtifacts artifacts: '**/target/*.jar, **/build/**, **/coverage/**', allowEmptyArchive: true
        }
    }
}