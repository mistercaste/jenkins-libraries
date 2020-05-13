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
                    build job: '../LIB/firds-database', parameters: []
                }
            }

            stage('jboss-fuse') {
                steps {
                    echo "JBOSS parameter (provided): [${pipelineParams.fuseParameter}]"
                    build job: '../LIB/firds-jboss-fuse', parameters: []
                }
            }

        }

        post {
            failure {
                mail to: pipelineParams.email, subject: 'Pipeline failed', body: "An error occurred"
            }
        }
    }
}