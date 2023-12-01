def call(String dockerRepoName, String imageName) {
    pipeline {
        agent any
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        }
        stages {
            stage('Python Lint') {
                steps {
                    sh 'pylint --fail-under=5 --disable=E0401 $(find . -name "*app.py")'
                }
            }

            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'DokcerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'allenlizz' -p '${TOKEN}' docker.io"
                        sh "cd ${dockerRepoName}"
                        sh "docker build -t allenlizz/${dockerRepoName}:${imageName} ${dockerRepoName}/."
                        sh "docker push allenlizz/${dockerRepoName}:${imageName}"
                    }
                }
            }

            stage('Security Scan') {
                steps {
                        sh 'pip install safety --break-system-packages'
                        sh 'safety check' // for Python dependency scan
                    script {
                        def services = ['receiver', 'storage', 'processing', 'audit_log']
                        services.each { service ->
                            dir("${service}") {
                                // Insert the correct commands for Python dependency scan and Docker image scan
                                // Ensure the Docker image name and tag are correctly specified
                                
                                sh "trivy image --severity HIGH,CRITICAL allenlizz/${service}:${imageName}" // for Docker image scan
                            }
                        }
                    }
                }
            }

            // Uncomment and complete the 'Deploy' stage when needed
            // stage('Deploy') {
            //     when {
            //         expression { params.DEPLOY }
            //     }
            //     steps {
            //         // Deploy steps go here
            //     }
            // }
        }
    }
}
