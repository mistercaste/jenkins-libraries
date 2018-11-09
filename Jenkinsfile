node {
    jobDsl scriptText: '''
multibranchPipelineJob('ESMA-FIRDS/example') {
    branchSources {
        github {
            repoOwner('mistercaste')
            repository('firds')
            checkoutCredentialsId('github')
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
        }
    }
}
'''
}