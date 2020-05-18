/*
This pipeline makes a sample use of the https://github.com/mistercaste/conference-app
In order to use it, please import this project in Jenkins as jenkins-lib, create a Pipeline Job and in the script section add the code below:

@Library('jenkins-lib') _
libPipelineConference {
    applicationFolder = 'app'
    branch = 'master'
    mavenGlobalSettingsId = 'maven-global-settings'
}
 */

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
                    step([$class    : 'XUnitPublisher',
                          thresholds: [
                                  [$class: 'FailedThreshold', failureNewThreshold: '', failureThreshold: '', unstableNewThreshold: '', unstableThreshold: ''],
                                  [$class: 'SkippedThreshold', failureNewThreshold: '', failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']],
                          tools     : [[$class: 'JUnitType',
                                        pattern: '**/target/surefire-reports/*.xml',
                                        skipNoTestFiles: 'true',
                                        failIfNotNew: 'true',
                                        deleteOutputFiles: 'true',
                                        stopProcessingIfError: 'true']]]
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
        }
    }
}