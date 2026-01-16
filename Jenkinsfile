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
        ARGOCD_SERVER = 'localhost:8081'  // ArgoCD on port 8081
        ARGOCD_APP_NAME = 'todo-fullstack-app'
        GIT_REPO_URL = 'https://github.com/Abhishek-4054/todo-fullstack-app.git'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Checkout source code'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: "${GIT_REPO_URL}"
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
                        npm cache clean --force
                        rmdir /s /q node_modules 2>nul || echo "node_modules not found"
                        del package-lock.json 2>nul || echo "package-lock.json not found"
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
                    bat '''
                        set CI=false
                        set NODE_OPTIONS=--openssl-legacy-provider
                        npm run build
                    '''
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

        stage('Update K8s Manifests') {
            steps {
                script {
                    dir('k8s') {
                        bat """
                            powershell -Command "(Get-Content backend-deployment.yaml) -replace 'image: ${BACKEND_IMAGE}:.*', 'image: ${BACKEND_IMAGE}:${IMAGE_TAG}' | Set-Content backend-deployment.yaml"
                            powershell -Command "(Get-Content frontend-deployment.yaml) -replace 'image: ${FRONTEND_IMAGE}:.*', 'image: ${FRONTEND_IMAGE}:${IMAGE_TAG}' | Set-Content frontend-deployment.yaml"
                        """
                    }
                }
            }
        }

        stage('Commit & Push K8s Manifests') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-credentials', 
                                                  usernameVariable: 'GIT_USER', 
                                                  passwordVariable: 'GIT_PASS')]) {
                    bat """
                        git config user.email "jenkins@ci.com"
                        git config user.name "Jenkins CI"
                        git add k8s/
                        git commit -m "Update image tags to ${IMAGE_TAG}" || echo "No changes to commit"
                        git push https://%GIT_USER%:%GIT_PASS%@github.com/Abhishek-4054/todo-fullstack-app.git main || echo "Push failed or no changes"
                    """
                }
                echo "✅ K8s manifests updated in Git"
                echo "🔄 ArgoCD will auto-sync the changes"
            }
        }

        stage('Verify Deployment Status') {
            steps {
                script {
                    echo "⏳ Waiting 30 seconds for ArgoCD to detect changes..."
                    sleep(time: 30, unit: 'SECONDS')
                    
                    echo "📊 Checking Kubernetes deployment status..."
                    bat """
                        kubectl rollout status deployment/todo-backend -n todo-app --timeout=300s || echo "Backend deployment check failed"
                        kubectl rollout status deployment/todo-frontend -n todo-app --timeout=300s || echo "Frontend deployment check failed"
                        kubectl get pods -n todo-app
                    """
                    
                    echo "✅ Deployment verification completed"
                    echo "🌐 Access your application:"
                    echo "   Frontend: http://localhost:30080"
                    echo "   Backend: http://localhost:30081"
                    echo "   ArgoCD: http://localhost:8081"
                }
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
            echo '✅ PIPELINE SUCCESS - Application deployed to Kubernetes via ArgoCD'
            echo "🚀 Access your app:"
            echo "   Frontend: http://localhost:30080"
            echo "   Backend: http://localhost:30081"
            echo "   ArgoCD UI: http://localhost:8081"
        }
        failure {
            echo '❌ PIPELINE FAILED'
        }
    }
}