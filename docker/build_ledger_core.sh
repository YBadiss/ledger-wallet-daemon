#!/usr/bin/env bash
set -euxo pipefail

# Clone the lib-core at the target commit
git clone --recurse-submodules https://github.com/LedgerHQ/lib-ledger-core.git
(cd lib-ledger-core && git remote add khalil https://github.com/KhalilBellakrid/lib-ledger-core.git && git fetch khalil)
(cd lib-ledger-core && git reset --hard "$(head -n 1 ../ledger_core_commit)" && head -n -1 .gitmodules && echo "	url = https://github.com/LedgerHQ/ethash.git" >> .gitmodules)
(cd lib-ledger-core && git submodule sync && git submodule update --init)

# Install the deps required to build the lib-core
echo "deb http://ftp.debian.org/debian stretch-backports main" >> /etc/apt/sources.list.d/sources.list
apt-get update
apt-get -t stretch-backports install -y g++ make cmake

# Build and package the lib-core
cmake --version
mkdir lib-ledger-core-build
(cd lib-ledger-core-build && cmake -DBUILD_TESTS=OFF -DTARGET_JNI=ON ../lib-ledger-core && make)
./package_libcore.sh linux

exit 0
