// Define a function that encapsulates your pipeline logic
def call(dockerRepoName, imageName) {
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

            
            // stage('Security') {
            //     steps {
            //         // Security steps go here
            //     }
            // }
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'DokcerHub', variable: 'TOKEN')]) {
                        sh "docker login -u 'allenlizz' -p '${TOKEN}' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag allenlizz/${dockerRepoName}:${imageName} ."
                        sh "docker push allenlizz/${dockerRepoName}:${imageName}"
                    }
                }
            }

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
