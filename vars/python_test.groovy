def call(String dockerRepoName, String imageName, String serviceToScan) {
    pipeline {
        agent any

        parameters {
        booleanParam(defaultValue: false, description: 'Deploy the App', name:
        'DEPLOY')
        }


        stages {
            stage('Python Lint') {
                steps {
                    script {
                        dir("${serviceToScan}") {
                            sh 'pylint --fail-under=5 --disable=E0401 $(find . -name "*app.py")'
                        }
                    }
                }
            }

            stage('Security Scan') {
                steps {
                    script {
                        dir("${serviceToScan}") {
                            // Echo vulnerabilities and ignore specified ones
                            def safetyOutput = sh(script: 'safety check --ignore 51668 --ignore 59473 --ignore 53048', returnStdout: true).trim()
                            echo "Safety check output for ${serviceToScan}: \n${safetyOutput}"
                        }
                    }
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

            
            stage('Deploy') {
                when {
                    expression { params.DEPLOY == true }
                }
                steps {
                        sshagent(['3855_vm']) {
                            sh """
                                ssh -o StrictHostKeyChecking=no azureuser@allenliacit3855.eastus.cloudapp.azure.com 'docker-compose -f /home/azureuser/acit3855/acit3855_lab7/deploy_jenkins/docker-compose.yml pull receiver storage processing audit_log && docker-compose -f /home/azureuser/acit3855/acit3855_lab7/deploy_jenkins/docker-compose.yml up -d'
                            """
                        }
                    }
                }

        }
    }
}
