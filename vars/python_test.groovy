def call(String dockerRepoName, String imageName, String service) {
    pipeline {
        agent any
        // parameters {
        //     booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        // }
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
                       
                    script {
                        def services = ['receiver', 'storage', 'processing', 'audit_log']
                        services.each { service ->
                            dir("${service}") {
                                // Echo vulnerabilities and ignore specified ones
                                def safetyOutput = sh(script: 'safety check --ignore 51668 --ignore 59473 --ignore 53048', returnStdout: true).trim()
                                echo "Safety check output for ${service}: \n${safetyOutput}"
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
