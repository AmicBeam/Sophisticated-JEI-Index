#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: scripts/build_all_versions.sh [version...]

Build one or more independent Sophisticated JEI Index Gradle projects.

Versions:
  1.20.1   Forge, JDK 17, directory versions/1.20.1
  1.21.1   NeoForge, JDK 21, directory versions/1.21.1
  26.1.2   NeoForge, JDK 25, directory versions/26.1.2

With no arguments, every version is built.

JDK selection, in order:
  1. SJI_JAVA_17_HOME / SJI_JAVA_21_HOME / SJI_JAVA_25_HOME
  2. JAVA_HOME, only if it already matches the required major version

Set all three SJI_JAVA_*_HOME variables when building every version from one
shell. JAVA_HOME is a last-resort fallback for a single matching version, not
a way to reuse one JDK for all three targets.

Examples:
  SJI_JAVA_17_HOME=/path/to/jdk-17 \
  SJI_JAVA_21_HOME=/path/to/jdk-21 \
  SJI_JAVA_25_HOME=/path/to/jdk-25 \
  ./scripts/build_all_versions.sh

  SJI_JAVA_21_HOME=/path/to/jdk-21 ./scripts/build_all_versions.sh 1.21.1
EOF
}

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "${script_dir}/.." && pwd)

if [ "${1-}" = "-h" ] || [ "${1-}" = "--help" ]; then
  usage
  exit 0
fi

requested=()
if [ "$#" -eq 0 ]; then
  requested=(1.20.1 1.21.1 26.1.2)
else
  requested=("$@")
fi

java_major() {
  local jdk_home=$1
  local java_bin="${jdk_home}/bin/java"
  if [ ! -x "${java_bin}" ]; then
    echo "error: ${java_bin} is missing or not executable" >&2
    return 1
  fi
  local version_line=$("${java_bin}" -version 2>&1 | awk 'NR==1 {print; exit}')
  local major
  major=$(printf '%s\n' "${version_line}" | sed -n 's/.* version "\([0-9][0-9]*\).*/\1/p')
  if [ -z "${major}" ]; then
    echo "error: could not parse Java major version from: ${version_line}" >&2
    return 1
  fi
  printf '%s\n' "${major}"
}

resolve_java_home() {
  version=$1
  required_major=$2
  preferred_var=$3
  preferred_home=""
  case "${preferred_var}" in
    SJI_JAVA_17_HOME) preferred_home=${SJI_JAVA_17_HOME-} ;;
    SJI_JAVA_21_HOME) preferred_home=${SJI_JAVA_21_HOME-} ;;
    SJI_JAVA_25_HOME) preferred_home=${SJI_JAVA_25_HOME-} ;;
    *)
      echo "error: internal JDK variable '${preferred_var}' is not supported" >&2
      return 1
      ;;
  esac

  if [ -n "${preferred_home}" ]; then
    major=$(java_major "${preferred_home}") || return 1
    if [ "${major}" != "${required_major}" ]; then
      echo "error: ${preferred_var}=${preferred_home} reports Java ${major}, expected ${required_major} for ${version}" >&2
      return 1
    fi
    printf '%s\n' "${preferred_home}"
    return 0
  fi

  if [ -n "${JAVA_HOME-}" ]; then
    major=$(java_major "${JAVA_HOME}") || return 1
    if [ "${major}" != "${required_major}" ]; then
      echo "error: ${version} needs JDK ${required_major}; ${preferred_var} is unset and JAVA_HOME=${JAVA_HOME} reports Java ${major}" >&2
      echo "Set ${preferred_var} to a JDK ${required_major} home, or point JAVA_HOME at JDK ${required_major} for this version only." >&2
      return 1
    fi
    printf '%s\n' "${JAVA_HOME}"
    return 0
  fi

  echo "error: ${version} needs JDK ${required_major}; set ${preferred_var} or JAVA_HOME" >&2
  return 1
}

build_version() {
  version=$1
  case "${version}" in
    1.20.1)
      required_major=17
      preferred_var=SJI_JAVA_17_HOME
      ;;
    1.21.1)
      required_major=21
      preferred_var=SJI_JAVA_21_HOME
      ;;
    26.1.2)
      required_major=25
      preferred_var=SJI_JAVA_25_HOME
      ;;
    *)
      echo "error: unknown version '${version}'" >&2
      usage >&2
      return 1
      ;;
  esac

  project_dir="${repo_root}/versions/${version}"
  wrapper="${project_dir}/gradlew"
  if [ ! -d "${project_dir}" ]; then
    echo "error: missing project directory ${project_dir}" >&2
    return 1
  fi
  if [ ! -x "${wrapper}" ]; then
    echo "error: missing executable Gradle wrapper ${wrapper}" >&2
    return 1
  fi

  java_home=$(resolve_java_home "${version}" "${required_major}" "${preferred_var}") || return 1
  echo "Building ${version} with JAVA_HOME=${java_home}"
  (
    cd "${project_dir}"
    env JAVA_HOME="${java_home}" PATH="${java_home}/bin:${PATH}" ./gradlew --no-daemon build
  )
}

for version in "${requested[@]}"; do
  build_version "${version}"
done
