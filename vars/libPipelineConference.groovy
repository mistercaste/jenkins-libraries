def call(body) {

    // evaluate the body block, and collect configuration into the object
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        environment {
            APPLICATION_FOLDER = "${pipelineParams.applicationFolder}"
            BRANCH = "${pipelineParams.branch}"
            MAVEN_GLOBAL_SETTINGS_ID = "${pipelineParams.mavenGlobalSettingsId}"
            SCM_URL = 'https://github.com/mistercaste/conference-app'
            REPOSITORY_URL = 'http://nexus:8081/repository/maven-releases/'
        }
        parameters {
            booleanParam(name: 'BUILD_DOCKER', defaultValue: false, description: 'Deploy to Docker?')
        }
        tools {
            maven 'Maven 3.3.3'
            jdk 'JDK 8'
        }
        stages {
            stage('Checkout') {
                steps {
                    git branch: BRANCH, credentialsId: 'GitCredentials', url: SCM_URL
                }
            }
            stage('Build') {
                steps {
                    configFileProvider([configFile(fileId: MAVEN_GLOBAL_SETTINGS_ID, variable: 'MAVEN_GLOBAL_SETTINGS')]) {
                        sh 'mvn -gs $MAVEN_GLOBAL_SETTINGS -f ${APPLICATION_FOLDER}/pom.xml clean versions:set -DnewVersion=DEV-${BUILD_NUMBER}'
                    }
                }
            }
            stage('Deploy') {
                steps {
                    configFileProvider([configFile(fileId: MAVEN_GLOBAL_SETTINGS_ID, variable: 'MAVEN_GLOBAL_SETTINGS')]) {
                        sh 'mvn -gs $MAVEN_GLOBAL_SETTINGS -f ${APPLICATION_FOLDER}/pom.xml clean deploy'
                    }
                }
            }
            stage('Test') {
                steps {
                    step([$class    : 'XUnitBuilder',
                          thresholds: [
                                  [$class: 'FailedThreshold', failureNewThreshold: '', failureThreshold: '', unstableNewThreshold: '', unstableThreshold: ''],
                                  [$class: 'SkippedThreshold', failureNewThreshold: '', failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']],
                          tools     : [[$class: 'JUnitType', pattern: '**/target/surefire-reports/*.xml']]]
                    )
                }
            }
            stage('Sonar') {
                steps {
                    configFileProvider([configFile(fileId: MAVEN_GLOBAL_SETTINGS_ID, variable: 'MAVEN_GLOBAL_SETTINGS')]) {
                        sh 'mvn -gs $MAVEN_GLOBAL_SETTINGS -f ${APPLICATION_FOLDER}/pom.xml org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent install -Psonar'
                        sh 'mvn -gs $MAVEN_GLOBAL_SETTINGS -f ${APPLICATION_FOLDER}/pom.xml sonar:sonar -Psonar'
                    }
                }
            }
            stage('Docker Build') {
                when {
                    expression { params.BUILD_DOCKER == 'true' }
                }
                steps {
                    sh 'echo "Building Docker Container . . ."'
                    sh 'cd ${APPLICATION_FOLDER} && sudo /usr/bin/docker build -t conference-${APPLICATION_FOLDER} .'
                    sh 'echo "Stopping Docker Container first"'
                    sh 'sudo /usr/bin/docker stop $(sudo /usr/bin/docker ps -a -q --filter="name=conference-${APPLICATION_FOLDER}") | true '
                    sh 'sudo /usr/bin/docker rm $(sudo /usr/bin/docker ps -a -q --filter="name=conference-${APPLICATION_FOLDER}") | true '
                    sh 'echo "Starting Docker Container"'
                    sh 'sudo /usr/bin/docker run -d --name conference-${APPLICATION_FOLDER} -p=48080:8080 conference-${APPLICATION_FOLDER}'
                }
            }
/*
        stage('Docker Stop') {
            steps {
                sh 'sudo /usr/bin/docker stop $(sudo /usr/bin/docker ps -a -q --filter="name=conference-${APPLICATION_FOLDER}")'
                sh 'sudo /usr/bin/docker rm $(sudo /usr/bin/docker ps -a -q --filter="name=conference-${APPLICATION_FOLDER}")'
        }
*/
        }
    }
}