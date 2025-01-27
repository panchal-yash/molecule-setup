    pipeline {
    agent {
        label 'min-bookworm-x64'
    }
    environment {
        product_to_test = "${params.product_to_test}"
        git_repo = "${params.git_repo}"
        install_repo = "${params.install_repo}"
        action_to_test  = "${params.action_to_test}"
        check_warnings = "${params.check_warnings}"
        install_mysql_shell = "${params.install_mysql_shell}"
    }
    parameters {
        choice(
            choices: [
                'install',
                'upgrade'
            ],
            description: 'Action To Test',
            name: 'action_to_test'
        )
    }
    options {
        withCredentials(moleculePdpsJenkinsCreds())
    }

        stages {
            stage('Set Build Name'){
                steps {
                    script {
                        currentBuild.displayName = "${env.BUILD_NUMBER}-${product_to_test}-${action_to_test}"
                    }
                }
            }

            stage('Checkout') {
                steps {
                    deleteDir()
                    git poll: false, branch: "master", url: "https://github.com/panchal-yash/molecule-setup.git"
                }
            }

            stage('Prepare') {
                steps {
                    script {
                        installMolecule()
                    }
                }
            }
            stage('RUN TESTS') {
                        steps {
                            script {
                                
                                moleculeParallelTest(OperatingSystems(), "molecule/ps/")
                                
                            }
                        }
            }

        }
    }

def OperatingSystems() {
    return ["ubuntu-focal","ubuntu-jammy","debian-12"]
}


def moleculeParallelTest(operatingSystems, moleculeDir) {
    tests = [:]
    operatingSystems.each { os ->
        tests["${os}"] =  {
            stage("${os}") {
                sh """
                    . virtenv/bin/activate
                    cd ${moleculeDir}
                    molecule test -s ${os}
                """
            }
        }
    }
    parallel tests
}

def installMolecule() {
        sh """
            sudo apt update -y
            sudo apt install -y python3 python3-pip python3-dev python3-venv
            python3 -m venv virtenv
            . virtenv/bin/activate
            python3 --version
            python3 -m pip install --upgrade pip
            python3 -m pip install --upgrade setuptools
            python3 -m pip install --upgrade setuptools-rust
            python3 -m pip install --upgrade PyYaml==5.3.1 molecule==3.3.0 testinfra pytest molecule-ec2==0.3 molecule[ansible] "ansible<10.0.0" "ansible-lint>=5.1.1,<6.0.0" boto3 boto
        """
}

def loadEnvFile(envFilePath) {
    def envMap = []
    def envFileContent = readFile(file: envFilePath).trim().split('\n')
    envFileContent.each { line ->
        if (line && !line.startsWith('#')) {
            def parts = line.split('=')
            if (parts.length == 2) {
                envMap << "${parts[0].trim()}=${parts[1].trim()}"
            }
        }
    }
    return envMap
}

