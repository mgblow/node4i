#1 Platform Stage
cd ~/apps/node4i/platform
git checkout development-1.2
git pull --rebase origin development-1.2
docker build -t ua-platform:latest .
## Run Image --->
docker run --network="host" -d --name node4i-platform \
-p 9193:9193 node4i-platform:latest


#2 Client Stage
cd ~/apps/node4i/client
git checkout development-1.2
git pull --rebase origin development-1.2
docker build -t ua-client:latest .
## Run Image --->
docker run --network="host" -d --name node4i-client \
-p 9992:9992 node4i-client:latest


#3 Dashboard Stage
cd ~/apps/node4i/dashboard
git checkout development-1.2
git pull --rebase origin development-1.2
docker build -t ua-dashboard:latest .
## Run Image --->
docker run --network="host" -d --name node4i-dashboard \
-e VUE_APP_MQTT_HOST="[MQTT_IP]" \
-e VUE_APP_MQTT_PORT="[MQTT_PORT]" \
-e VUE_APP_SERVER_URI="http://[CLIENT_IP]:[CLIENT_PORT]" \
-e VUE_APP_POD_URI="https://accounts.pod.ir" \
-e VUE_APP_POD_CLIENT_ID="4204ecb244adbc50c0e6dd24e6e0" \
-e VUE_APP_POD_CLIENT_SECRET="01ae1d77" \
-e VUE_APP_SINGIN_DRIVER="node4i_ACCOUNT" \
-p 8080:8080 node4i-dashboard:latest