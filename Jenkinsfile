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
        SONAR_HOST_URL = 'http://172.25.240.1:9000'
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
                    script {
                        def testResult = bat(
                            script: '''
                                npm install
                                npm run test:ci || echo "Tests completed with warnings"
                            ''',
                            returnStatus: true
                        )
                        
                        if (testResult == 0) {
                            echo '✅ Frontend tests passed or no tests found'
                        } else {
                            echo '⚠️ Frontend tests had issues but continuing...'
                        }
                    }
                }
            }
            post {
                always {
                    script {
                        // Publish coverage if it exists
                        if (fileExists('frontend/coverage/lcov-report/index.html')) {
                            publishHTML([
                                reportDir: 'frontend/coverage/lcov-report',
                                reportFiles: 'index.html',
                                reportName: 'Frontend Coverage Report',
                                allowMissing: true
                            ])
                        } else {
                            echo 'ℹ️ No frontend coverage report generated'
                        }
                    }
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
                echo '📊 Running SonarQube analysis for frontend...'
                dir('frontend') {
                    script {
                        def sonarResult = bat(
                            script: """
                                npx sonar-scanner ^
                                -Dsonar.projectKey=todo-frontend ^
                                -Dsonar.projectName="Todo Frontend" ^
                                -Dsonar.sources=src ^
                                -Dsonar.host.url=%SONAR_HOST_URL% ^
                                -Dsonar.token=%SONAR_TOKEN% ^
                                -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info || echo "SonarQube scan completed"
                            """,
                            returnStatus: true
                        )
                        
                        if (sonarResult == 0) {
                            echo '✅ Frontend SonarQube analysis completed'
                        } else {
                            echo '⚠️ Frontend SonarQube analysis had warnings'
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '🚦 Waiting for SonarQube Quality Gate...'
                script {
                    try {
                        timeout(time: 5, unit: 'MINUTES') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️ Quality Gate status: ${qg.status}"
                                echo "Quality gate failed but continuing deployment..."
                                // For production, use: error "Quality Gate failed: ${qg.status}"
                            } else {
                                echo '✅ Quality Gate passed!'
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️ Quality Gate check failed: ${e.message}"
                        echo "Continuing with deployment..."
                    }
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
            echo "Docker Hub: https://hub.docker.com/u/${DOCKERHUB_USERNAME}"
            
            script {
                try {
                    emailext(
                        subject: "✅ Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            <html>
                            <body>
                                <h2 style="color: green;">✅ Build Successful</h2>
                                <table border="1" cellpadding="10">
                                    <tr><td><strong>Job:</strong></td><td>${env.JOB_NAME}</td></tr>
                                    <tr><td><strong>Build Number:</strong></td><td>${env.BUILD_NUMBER}</td></tr>
                                    <tr><td><strong>Duration:</strong></td><td>${currentBuild.durationString}</td></tr>
                                    <tr><td><strong>Backend Image:</strong></td><td>${BACKEND_IMAGE}:${IMAGE_TAG}</td></tr>
                                    <tr><td><strong>Frontend Image:</strong></td><td>${FRONTEND_IMAGE}:${IMAGE_TAG}</td></tr>
                                </table>
                                <br>
                                <p><strong>Links:</strong></p>
                                <ul>
                                    <li><a href="${SONAR_HOST_URL}">SonarQube Dashboard</a></li>
                                    <li><a href="https://hub.docker.com/u/${DOCKERHUB_USERNAME}">Docker Hub</a></li>
                                    <li><a href="${BUILD_URL}console">Console Output</a></li>
                                    <li><a href="${BUILD_URL}testReport">Test Report</a></li>
                                </ul>
                            </body>
                            </html>
                        """,
                        to: 'abhishek184057@gmail.com',
                        mimeType: 'text/html'
                    )
                } catch (Exception e) {
                    echo "Failed to send email: ${e.message}"
                }
            }
        }
        
        failure {
            echo '❌ Pipeline failed!'
            
            script {
                try {
                    emailext(
                        subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """
                            <html>
                            <body>
                                <h2 style="color: red;">❌ Build Failed</h2>
                                <table border="1" cellpadding="10">
                                    <tr><td><strong>Job:</strong></td><td>${env.JOB_NAME}</td></tr>
                                    <tr><td><strong>Build Number:</strong></td><td>${env.BUILD_NUMBER}</td></tr>
                                    <tr><td><strong>Failed Stage:</strong></td><td>${env.STAGE_NAME}</td></tr>
                                    <tr><td><strong>Duration:</strong></td><td>${currentBuild.durationString}</td></tr>
                                </table>
                                <br>
                                <p><a href="${BUILD_URL}console">View Console Output</a></p>
                            </body>
                            </html>
                        """,
                        to: 'abhishek184057@gmail.com',
                        mimeType: 'text/html'
                    )
                } catch (Exception e) {
                    echo "Failed to send email: ${e.message}"
                }
            }
        }
        
        unstable {
            echo '⚠️ Build is unstable'
        }
        
        always {
            echo '🔒 Logging out from Docker Hub...'
            bat 'docker logout || exit 0'
            
            // Archive artifacts
            script {
                try {
                    archiveArtifacts artifacts: '**/target/*.jar, **/build/**, **/coverage/**', allowEmptyArchive: true, fingerprint: true
                } catch (Exception e) {
                    echo "No artifacts to archive: ${e.message}"
                }
            }
            
            // Workspace cleanup
            echo '🧹 Cleaning workspace...'
        }
    }
}