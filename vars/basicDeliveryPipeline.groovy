def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        stages {

            stage('database') {
                steps {
                    build job: '/FIRDS/library/firds-database', parameters: []
                }
            }

            stage('jboss-fuse') {
                steps {
                    echo "Performing build: [${pipelineParams.buildMessageSample}]"
                }
            }

            stage ('test') {
                steps {
                    parallel (
                            "unit tests": { sh "echo -e Unit tests message: ${pipelineParams.unitTestsMessage}" },
                            "integration tests": { sh "echo -e Integration tests message: ${pipelineParams.integrationTestsMessage}" }
                    )
                }
            }

            stage('deploy'){
                steps {
                    echo "Performing deploy: [${pipelineParams.deployMessageSample}]"
                }
            }

        }

        post {
            failure {
                mail to: pipelineParams.email, subject: 'Pipeline failed', body: "Blah"
            }
        }
    }
}