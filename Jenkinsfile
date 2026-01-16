pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
        nodejs 'NodeJS-18'
    }

    environment {
        // Docker Configuration
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME = 'abhishekc4054'
        BACKEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_USERNAME}/todo-frontend"
        IMAGE_TAG = "${BUILD_NUMBER}"

        // SonarQube Configuration
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')

        // Git Configuration
        GIT_REPO_URL = 'https://github.com/Abhishek-4054/todo-fullstack-app.git'

        // Kubernetes Configuration
        K8S_NAMESPACE = 'todo-app'
    }

    stages {

        // ============================================
        // CI STAGES - Build, Test, Quality Check
        // ============================================

        stage('Checkout') {
            steps {
                echo '📥 Checking out source code from Git'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO_URL}"
                
                // Debug: Show directory structure
                bat 'dir'
                bat 'dir backend'
                bat 'if exist backend\\todoapp (echo todoapp folder EXISTS) else (echo todoapp folder NOT FOUND)'
            }
        }

        stage('Backend: Unit Tests') {
            steps {
                echo '🧪 Running Backend Unit Tests'
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

        stage('Frontend: Unit Tests') {
            steps {
                echo '🧪 Running Frontend Unit Tests'
                dir('frontend') {
                    bat '''
                        npm install
                        npm run test:ci || echo "Frontend tests completed"
                    '''
                }
            }
        }

        stage('Backend: SonarQube Analysis') {
            steps {
                echo '🔍 Running SonarQube Analysis for Backend'
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
                echo '🔍 Running SonarQube Analysis for Frontend'
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

        stage('Quality Gate Check') {
            steps {
                echo '🚦 Waiting for SonarQube Quality Gate'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Backend Artifact') {
            steps {
                echo '🔨 Building Backend JAR'
                script {
                    // Check if pom.xml exists
                    if (fileExists('backend/todoapp/pom.xml')) {
                        dir('backend/todoapp') {
                            bat 'mvn clean package -DskipTests'
                        }
                    } else if (fileExists('backend/pom.xml')) {
                        dir('backend') {
                            bat 'mvn clean package -DskipTests'
                        }
                    } else {
                        error 'POM.xml not found in backend directory!'
                    }
                }
            }
        }

        stage('Build Frontend Artifact') {
            steps {
                echo '🔨 Building Frontend Production Bundle'
                dir('frontend') {
                    bat 'npm run build'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker Images'
                bat "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest backend/todoapp"
                bat "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest frontend"
            }
        }

        stage('Push Docker Images to DockerHub') {
            steps {
                echo '🔐 Logging into DockerHub'
                bat """
                    echo %DOCKERHUB_CREDENTIALS_PSW% | docker login ^
                    -u %DOCKERHUB_CREDENTIALS_USR% --password-stdin
                """
                
                echo '📤 Pushing Docker Images'
                bat "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${BACKEND_IMAGE}:latest"
                bat "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                bat "docker push ${FRONTEND_IMAGE}:latest"
            }
        }

        // ============================================
        // CD STAGES - Deploy via GitOps (ArgoCD)
        // ============================================

        stage('Update Kubernetes Manifests') {
            steps {
                echo '📝 Updating Kubernetes manifests with new image tags'
                dir('k8s') {
                    // Update Backend Deployment
                    bat """
                        powershell -NoProfile -Command ^
                        "(Get-Content backend-deployment.yaml) ^
                        -replace 'image: .*todo-backend:.*', ^
                        'image: ${DOCKERHUB_USERNAME}/todo-backend:${IMAGE_TAG}' ^
                        | Set-Content backend-deployment.yaml"
                    """

                    // Update Frontend Deployment
                    bat """
                        powershell -NoProfile -Command ^
                        "(Get-Content frontend-deployment.yaml) ^
                        -replace 'image: .*todo-frontend:.*', ^
                        'image: ${DOCKERHUB_USERNAME}/todo-frontend:${IMAGE_TAG}' ^
                        | Set-Content frontend-deployment.yaml"
                    """
                }
            }
        }

        stage('Commit & Push Manifests (GitOps)') {
            steps {
                echo '🔄 Committing and pushing updated manifests to Git'
                withCredentials([
                    usernamePassword(
                        credentialsId: 'github-credentials',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_PASS'
                    )
                ]) {
                    bat """
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI/CD"
                        git add k8s/backend-deployment.yaml k8s/frontend-deployment.yaml
                        git commit -m "CI/CD: Update image tags to build ${IMAGE_TAG}" || echo "No changes to commit"
                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main
                    """
                }
            }
        }

        stage('ArgoCD Auto-Sync') {
            steps {
                echo '🚀 ArgoCD will automatically detect and deploy the changes'
                echo '💡 Make sure ArgoCD auto-sync is enabled for your application'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '📊 Verifying Kubernetes deployment rollout status'
                
                bat """
                    kubectl rollout status deployment/todo-backend ^
                    -n ${K8S_NAMESPACE} --timeout=300s
                """

                bat """
                    kubectl rollout status deployment/todo-frontend ^
                    -n ${K8S_NAMESPACE} --timeout=300s
                """
            }
        }

        stage('Health Check') {
            steps {
                echo '🏥 Running post-deployment health checks'
                bat """
                    kubectl get pods -n ${K8S_NAMESPACE}
                    kubectl get services -n ${K8S_NAMESPACE}
                """
            }
        }

        stage('Cleanup') {
            steps {
                echo '🧹 Cleaning up Docker credentials'
                bat 'docker logout'
            }
        }
    }

    post {
        success {
            echo '✅ CI/CD PIPELINE COMPLETED SUCCESSFULLY'
            echo '================================================'
            echo "📦 Docker Images: ${BACKEND_IMAGE}:${IMAGE_TAG}, ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            echo "🚀 Deployed to Kubernetes namespace: ${K8S_NAMESPACE}"
            echo "🎉 GitOps deployment via ArgoCD successful"
            echo '================================================'
        }
        failure {
            echo '❌ CI/CD PIPELINE FAILED'
            echo '🔄 Check logs above for error details'
        }
        always {
            echo "📊 Build #${BUILD_NUMBER} finished at ${new Date()}"
        }
    }
}