def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        stages {

            stage('database') {
                steps {
                    echo "Performing database upgrade. Connection string: [${pipelineParams.jdbcConnection}]"
                    sayHello 'Pipeline User'
                }
            }

            stage('jboss-fuse') {
                steps {
                    sh "echo -e Stopping, updating and restarting container: [${pipelineParams.unitTestsMessage}]"
                }
            }

        }

        post {
            failure {
                mail to: pipelineParams.email, subject: 'Pipeline failed', body: "Warning : the pipeline execution has failed"
            }
        }
    }
}
