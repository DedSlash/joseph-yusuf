import groovy.json.JsonSlurper

// ============================================================
//  ÉTAT GLOBAL
// ============================================================
def rollbackRequired = false
def deployedServices = []

// ============================================================
//  UTILITAIRES GIT
// ============================================================

def getLastTagCommit() {
    def lastTag = sh(returnStdout: true, script: """
        git tag --sort=-creatordate | grep '^v' | head -1 || true
    """).trim()
    if (!lastTag) return [tag: '', sha: '']
    def sha = sh(returnStdout: true, script: "git rev-list -n 1 ${lastTag} 2>/dev/null || true").trim()
    return [tag: lastTag, sha: sha]
}

def getChangedFiles(String ref) {
    def cmd = ref
        ? "git diff --name-only ${ref} HEAD 2>/dev/null || git diff --name-only HEAD~1 HEAD 2>/dev/null || git show --name-only --format='' HEAD 2>/dev/null || true"
        : "git diff --name-only HEAD~1 HEAD 2>/dev/null || git show --name-only --format='' HEAD 2>/dev/null || true"
    def raw = sh(returnStdout: true, script: cmd).trim()
    return raw ? raw.split('\n').toList() : []
}

def detectChangedServices(List<String> allServices, List<String> changedFiles) {
    if (!changedFiles) return []
    return allServices.findAll { svc ->
        changedFiles.any { it.startsWith("${svc}/") }
    }
}

def detectFrontendChanged(List<String> changedFiles) {
    if (!changedFiles) return false
    return changedFiles.any { it.startsWith("frontend/") }
}

// ============================================================
//  UTILITAIRE : Sauvegarder / Restaurer l'état du pipeline
// ============================================================

def saveState() {
    writeFile file: 'pipeline-state.properties', text: """SERVICES_TO_DEPLOY=${env.SERVICES_TO_DEPLOY ?: ''}
FRONTEND_TO_DEPLOY=${env.FRONTEND_TO_DEPLOY ?: 'false'}
SERVICES_TO_BUILD=${env.SERVICES_TO_BUILD ?: ''}
FRONTEND_TO_BUILD=${env.FRONTEND_TO_BUILD ?: 'false'}
SKIP_ALL=${env.SKIP_ALL ?: 'false'}
VERSION=${env.VERSION ?: ''}
GIT_COMMIT_SHORT=${env.GIT_COMMIT_SHORT ?: ''}"""
}

def restoreState() {
    unstash 'pipeline-state'
    def props = readProperties file: 'pipeline-state.properties'
    env.SERVICES_TO_DEPLOY = props.SERVICES_TO_DEPLOY ?: ''
    env.FRONTEND_TO_DEPLOY = props.FRONTEND_TO_DEPLOY ?: 'false'
    env.SERVICES_TO_BUILD  = props.SERVICES_TO_BUILD  ?: ''
    env.FRONTEND_TO_BUILD  = props.FRONTEND_TO_BUILD  ?: 'false'
    env.SKIP_ALL           = props.SKIP_ALL           ?: 'false'
    env.VERSION            = props.VERSION            ?: ''
    env.GIT_COMMIT_SHORT   = props.GIT_COMMIT_SHORT   ?: ''
    echo """State restored:
   VERSION            = ${env.VERSION}
   SKIP_ALL           = ${env.SKIP_ALL}
   SERVICES_TO_BUILD  = ${env.SERVICES_TO_BUILD  ?: '(none)'}
   SERVICES_TO_DEPLOY = ${env.SERVICES_TO_DEPLOY ?: '(none)'}
   FRONTEND_TO_BUILD  = ${env.FRONTEND_TO_BUILD}
   FRONTEND_TO_DEPLOY = ${env.FRONTEND_TO_DEPLOY}"""
}

// ============================================================
//  PIPELINE
// ============================================================
pipeline {
    agent any

    parameters {
        booleanParam(name: 'FORCE_BUILD', defaultValue: false,
            description: 'Forcer le rebuild de TOUS les services (back + front)')
        booleanParam(name: 'FORCE_DEPLOY', defaultValue: false,
            description: 'Rebuild les services modifies + deploie tout')
    }

    environment {
        VPS_SSH_KEY_ID = 'vps-ssh-key'
        BACKEND_SERVICES = 'discovery-server,gateway-service,auth-service,income-service,rule-engine-service'
        MAJOR_VERSION = '1'
        MINOR_VERSION = '0'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        // --------------------------------------------------------
        //  1. VERSION & DELTA
        // --------------------------------------------------------
        stage('Version & Delta') {
            steps {
                checkout scm
                script {
                    def gitCommit     = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def gitCommitFull = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    def allServices   = env.BACKEND_SERVICES.split(',').toList()

                    env.GIT_COMMIT_SHORT = gitCommit
                    env.SKIP_ALL         = 'false'

                    def tagInfo       = getLastTagCommit()
                    def lastTag       = tagInfo.tag
                    def lastTagSha    = tagInfo.sha
                    def hasNewCommits = !lastTag || (lastTagSha != gitCommitFull)

                    def isFeatureBranch = env.BRANCH_NAME.startsWith('feature/') || env.BRANCH_NAME.startsWith('phase/')

                    String newVersion
                    if (env.BRANCH_NAME == 'main') {
                        newVersion = "${env.MAJOR_VERSION}.${env.MINOR_VERSION}.${env.BUILD_NUMBER}"
                    } else if (isFeatureBranch) {
                        def feat = env.BRANCH_NAME.replaceAll('^(feature|phase)/', '').replaceAll('[^a-zA-Z0-9]', '-')
                        newVersion = "${env.MAJOR_VERSION}.${env.MINOR_VERSION}.${env.BUILD_NUMBER}-${feat}-${gitCommit}"
                    } else {
                        newVersion = "${env.MAJOR_VERSION}.${env.MINOR_VERSION}.${env.BUILD_NUMBER}-dev-${gitCommit}"
                    }

                    def changedFiles    = getChangedFiles(lastTag ?: '')
                    def changedServices = detectChangedServices(allServices, changedFiles)
                    def frontendChanged = detectFrontendChanged(changedFiles)

                    if (changedFiles) {
                        echo "Changed files since ${lastTag ?: 'beginning'} (${changedFiles.size()}):"
                        changedFiles.each { echo "   -> ${it}" }
                    } else {
                        echo "No diff detected"
                    }
                    echo "Changed services: ${changedServices ?: '(none)'}"
                    echo "Frontend changed: ${frontendChanged}"

                    // ── 3 MODES ──────────────────────────────────────────

                    def svcsToBuild    = []
                    def svcsToDeploy   = []
                    def buildFrontend  = false
                    def deployFrontend = false

                    if (params.FORCE_BUILD) {
                        echo "MODE: FORCE_BUILD — rebuild all"
                        svcsToBuild    = allServices.toList()
                        svcsToDeploy   = allServices.toList()
                        buildFrontend  = true
                        deployFrontend = true
                        env.VERSION    = newVersion
                    } else if (params.FORCE_DEPLOY) {
                        echo "MODE: FORCE_DEPLOY — rebuild changed + deploy all"
                        changedServices.each { svc ->
                            svcsToBuild  << svc
                            svcsToDeploy << svc
                        }
                        allServices.each { svc ->
                            if (!svcsToDeploy.contains(svc)) {
                                svcsToDeploy << svc
                            }
                        }
                        buildFrontend  = frontendChanged
                        deployFrontend = true
                        env.VERSION    = newVersion
                    } else {
                        echo "MODE: NORMAL — delta only"
                        if (!changedServices && !frontendChanged) {
                            echo "No changes detected — skipping pipeline"
                            env.SKIP_ALL           = 'true'
                            env.VERSION            = newVersion
                            env.SERVICES_TO_BUILD  = ''
                            env.SERVICES_TO_DEPLOY = ''
                            env.FRONTEND_TO_BUILD  = 'false'
                            env.FRONTEND_TO_DEPLOY = 'false'
                            saveState()
                            stash name: 'pipeline-state', includes: 'pipeline-state.properties'
                            return
                        }
                        svcsToBuild    = changedServices.toList()
                        svcsToDeploy   = changedServices.toList()
                        buildFrontend  = frontendChanged
                        deployFrontend = frontendChanged
                        env.VERSION    = newVersion
                    }

                    env.SERVICES_TO_BUILD  = svcsToBuild.join(',')
                    env.SERVICES_TO_DEPLOY = svcsToDeploy.join(',')
                    env.FRONTEND_TO_BUILD  = buildFrontend  ? 'true' : 'false'
                    env.FRONTEND_TO_DEPLOY = deployFrontend ? 'true' : 'false'

                    echo """
=== SUMMARY ===
Version          : ${env.VERSION}
Commit           : ${gitCommit}
Branch           : ${env.BRANCH_NAME}
Mode             : ${params.FORCE_BUILD ? 'FORCE_BUILD' : params.FORCE_DEPLOY ? 'FORCE_DEPLOY' : 'NORMAL'}
Services to build: ${env.SERVICES_TO_BUILD ?: '(none)'}
Services to deploy: ${env.SERVICES_TO_DEPLOY ?: '(none)'}
Frontend build   : ${env.FRONTEND_TO_BUILD}
Frontend deploy  : ${env.FRONTEND_TO_DEPLOY}
==============="""

                    saveState()
                    stash name: 'pipeline-state', includes: 'pipeline-state.properties'
                }
            }
        }

        // --------------------------------------------------------
        //  2. TESTS + SONARQUBE (feature/* and phase/* only)
        // --------------------------------------------------------
        stage('Test & Quality Gate') {
            when {
                expression { env.BRANCH_NAME.startsWith('feature/') || env.BRANCH_NAME.startsWith('phase/') }
            }
            steps {
                script {
                    restoreState()

                    if (env.SKIP_ALL == 'true' ||
                        (!env.SERVICES_TO_BUILD?.trim() && env.FRONTEND_TO_BUILD != 'true')) {
                        echo "Nothing to test — skipping"
                        return
                    }

                    def servicesToTest = env.SERVICES_TO_BUILD?.trim()
                        ? env.SERVICES_TO_BUILD.split(',').toList()
                        : []

                    if (servicesToTest) {
                        def backendTests = [:]
                        servicesToTest.each { svc ->
                            def svcName = svc
                            backendTests["Test ${svcName}"] = {
                                dir(svcName) {
                                    sh "mvn clean test -B"
                                    withSonarQubeEnv('SonarQube') {
                                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                                            sh """
                                                mvn verify sonar:sonar \
                                                    -Dsonar.projectKey=joseph-${svcName} \
                                                    -Dsonar.projectName='joseph-${svcName}' \
                                                    -Dsonar.projectVersion=${env.VERSION} \
                                                    -Dsonar.token=\${SONAR_TOKEN}
                                            """
                                        }
                                    }
                                    timeout(time: 2, unit: 'MINUTES') {
                                        def qg = waitForQualityGate()
                                        if (qg.status != 'OK') {
                                            error "Quality Gate FAILED for ${svcName}: ${qg.status}"
                                        }
                                        echo "Quality Gate OK — ${svcName}"
                                    }
                                }
                            }
                        }
                        parallel backendTests
                    }

                    if (env.FRONTEND_TO_BUILD == 'true') {
                        dir('frontend') {
                            sh "npm ci && npx ng test --watch=false --browsers=ChromeHeadless --code-coverage"
                            withSonarQubeEnv('SonarQube') {
                                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                                    sh """
                                        npx sonar-scanner \
                                            -Dsonar.projectKey=joseph-frontend \
                                            -Dsonar.projectName='joseph-frontend' \
                                            -Dsonar.projectVersion=${env.VERSION} \
                                            -Dsonar.token=\${SONAR_TOKEN}
                                    """
                                }
                            }
                            timeout(time: 2, unit: 'MINUTES') {
                                def qgF = waitForQualityGate()
                                if (qgF.status != 'OK') {
                                    error "Quality Gate FAILED for frontend: ${qgF.status}"
                                }
                                echo "Quality Gate OK — frontend"
                            }
                        }
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // --------------------------------------------------------
        //  3. BUILD MAVEN (main only)
        // --------------------------------------------------------
        stage('Build') {
            when {
                branch 'main'
            }
            steps {
                script {
                    restoreState()

                    if (env.SKIP_ALL == 'true' || !env.SERVICES_TO_BUILD?.trim()) {
                        echo "Nothing to build — skipping"
                        return
                    }

                    def servicesToBuild = env.SERVICES_TO_BUILD.split(',').toList()
                    def moduleList = servicesToBuild.join(',')

                    sh "mvn clean package -DskipTests -B -pl ${moduleList} -am"
                }
            }
        }

        // --------------------------------------------------------
        //  4. DOCKER BUILD & PUSH (main only)
        // --------------------------------------------------------
        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                script {
                    restoreState()

                    if (env.SKIP_ALL == 'true' ||
                        (!env.SERVICES_TO_BUILD?.trim() && env.FRONTEND_TO_BUILD != 'true')) {
                        echo "Nothing to dockerize — skipping"
                        return
                    }

                    withCredentials([
                        string(credentialsId: 'registry-url', variable: 'REGISTRY_URL'),
                        string(credentialsId: 'registry-user', variable: 'REGISTRY_USER'),
                        string(credentialsId: 'registry-pass', variable: 'REGISTRY_PASS')
                    ]) {
                        sh "echo \${REGISTRY_PASS} | docker login \${REGISTRY_URL} -u \${REGISTRY_USER} --password-stdin"

                        def servicesToBuild = env.SERVICES_TO_BUILD?.trim()
                            ? env.SERVICES_TO_BUILD.split(',').toList()
                            : []

                        servicesToBuild.each { svc ->
                            dir(svc) {
                                sh """
                                    docker build -t \${REGISTRY_URL}/joseph-${svc}:${BUILD_NUMBER} -t \${REGISTRY_URL}/joseph-${svc}:latest .
                                    docker push \${REGISTRY_URL}/joseph-${svc}:${BUILD_NUMBER}
                                    docker push \${REGISTRY_URL}/joseph-${svc}:latest
                                """
                            }
                            deployedServices.add("joseph-${svc}:${BUILD_NUMBER}")
                        }

                        if (env.FRONTEND_TO_BUILD == 'true') {
                            dir('frontend') {
                                sh """
                                    docker build -t \${REGISTRY_URL}/joseph-frontend:${BUILD_NUMBER} -t \${REGISTRY_URL}/joseph-frontend:latest .
                                    docker push \${REGISTRY_URL}/joseph-frontend:${BUILD_NUMBER}
                                    docker push \${REGISTRY_URL}/joseph-frontend:latest
                                """
                            }
                            deployedServices.add("joseph-frontend:${BUILD_NUMBER}")
                        }

                        sh "docker logout \${REGISTRY_URL} || true"
                    }
                }
            }
        }

        // --------------------------------------------------------
        //  5. DEPLOY ANSIBLE (main only)
        // --------------------------------------------------------
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                script {
                    restoreState()

                    if (env.SKIP_ALL == 'true' ||
                        (!env.SERVICES_TO_DEPLOY?.trim() && env.FRONTEND_TO_DEPLOY != 'true')) {
                        echo "Nothing to deploy — skipping"
                        return
                    }

                    withCredentials([string(credentialsId: 'registry-url', variable: 'REGISTRY_URL')]) {
                        sshagent(credentials: [env.VPS_SSH_KEY_ID]) {
                            sh """
                                cd ansible && ansible-playbook playbooks/deploy.yml \
                                    -e build_number=${BUILD_NUMBER} \
                                    -e registry_url=\${REGISTRY_URL} \
                                    -e services_to_deploy=${env.SERVICES_TO_DEPLOY ?: ''} \
                                    -e deploy_frontend=${env.FRONTEND_TO_DEPLOY} \
                                    --vault-password-file vault/.vault_pass
                            """
                        }
                    }
                }
            }
        }

        // --------------------------------------------------------
        //  6. HEALTH CHECKS (main only)
        // --------------------------------------------------------
        stage('Health Checks') {
            when {
                branch 'main'
            }
            steps {
                script {
                    restoreState()

                    if (env.SKIP_ALL == 'true' ||
                        (!env.SERVICES_TO_DEPLOY?.trim() && env.FRONTEND_TO_DEPLOY != 'true')) {
                        echo "Nothing to check — skipping"
                        return
                    }

                    withCredentials([string(credentialsId: 'vps-host', variable: 'VPS_HOST')]) {
                        def servicesToCheck = env.SERVICES_TO_DEPLOY?.trim()
                            ? env.SERVICES_TO_DEPLOY.split(',').toList()
                            : []

                        def portMap = [
                            'discovery-server'   : 8761,
                            'gateway-service'    : 8080,
                            'auth-service'       : 8081,
                            'income-service'     : 8082,
                            'rule-engine-service': 8083
                        ]

                        servicesToCheck.each { svc ->
                            def port = portMap[svc]
                            if (port) {
                                sh "curl -sf --retry 5 --retry-delay 5 http://\${VPS_HOST}:${port}/actuator/health || exit 1"
                                echo "Health OK — ${svc}"
                            }
                        }

                        if (env.FRONTEND_TO_DEPLOY == 'true') {
                            sh "curl -sf --retry 5 --retry-delay 5 http://\${VPS_HOST}:4200 || exit 1"
                            echo "Health OK — frontend"
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    //  POST
    // ============================================================
    post {
        failure {
            node('master') {
                script {
                    if (env.BRANCH_NAME == 'main') {
                        rollbackRequired = true
                        echo "Deployment failed — triggering rollback"
                        try {
                            withCredentials([string(credentialsId: 'registry-url', variable: 'REGISTRY_URL')]) {
                                sshagent(credentials: [env.VPS_SSH_KEY_ID]) {
                                    sh """
                                        cd ansible && ansible-playbook playbooks/rollback.yml \
                                            -e registry_url=\${REGISTRY_URL} \
                                            -e services_to_rollback=${env.SERVICES_TO_DEPLOY ?: ''} \
                                            -e rollback_frontend=${env.FRONTEND_TO_DEPLOY ?: 'false'} \
                                            --vault-password-file vault/.vault_pass
                                    """
                                }
                            }
                            echo "Rollback completed"
                        } catch (Exception e) {
                            echo "Rollback failed: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
        success {
            echo "Pipeline SUCCESS — v${env.VERSION} on ${env.BRANCH_NAME}"
        }
        always {
            node('master') {
                cleanWs()
            }
        }
    }
}
