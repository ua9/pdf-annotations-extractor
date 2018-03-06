#!/bin/sh

# TODO after this bug will be fixed: https://youtrack.jetbrains.com/issue/IDEA-168872
# TODO this file can be removed

script_dir=$(cd "$(dirname "$0")"; pwd)
project_dir_name=$(basename "$script_dir")

cd "$script_dir"

docker rmi "$project_dir_name" --force --no-prune || true \
    && docker build --tag "$project_dir_name" . \
    && docker kill "$project_dir_name" || true \
    && docker rm --force "$project_dir_name" || true \
    && docker run --rm --publish 5005:5005 --detach --name "$project_dir_name" "$project_dir_name" \
        java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 \
        -jar app.jar "$@" \
    && sleep 1
