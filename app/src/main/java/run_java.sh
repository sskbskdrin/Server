export CLASSPATH="/data/app/cn.sskbskdrin.server-1/base.apk"
exec app_process32 /system/bin cn.sskbskdrin.client.Main http:8080

export CLASSPATH="/data/app/cn.sskbskdrin.server-2/base.apk"
exec app_process32 /system/bin cn.sskbskdrin.client.Main http:8080

sh -c CLASSPATH="/data/app/cn.sskbskdrin.server-1/base.apk" app_process32 /system/bin cn.sskbskdrin.client.Main http:8080