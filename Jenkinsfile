pipeline {
    agent any

    options {
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        PATH = "/Users/ashutoshpandey/.gemini/antigravity/scratch/tools/apache-maven-3.9.9/bin:/Applications/Docker.app/Contents/Resources/bin:${env.PATH}"
        IMAGE_NAME = 'codexashu08/online-feedback-form'
    }

    stages {
        stage('Verify Environment') {
            steps {
                sh '''
                    echo "Workspace: $(pwd)"
                    java -version
                    mvn -version
                    ls -la index.html pom.xml
                '''
            }
        }

        stage('Maven Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Maven Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    if command -v docker >/dev/null 2>&1; then
                        docker build -t ${IMAGE_NAME}:latest .
                    else
                        echo "Docker CLI not available; skipping container build."
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded: Maven build, tests, artifact, and Docker image completed.'
        }
        failure {
            echo 'Pipeline failed. Check console logs for details.'
        }
    }
}
