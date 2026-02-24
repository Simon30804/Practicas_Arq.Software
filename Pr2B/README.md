# EJECUCIÓN EN MÁQUINA LOCAL
# Compilar todo:
javac *.java

# Para matar los rmiregistry anterior (en terminal bash de )
taskkill //F //IM rmiregistry.exe

# MÁQUINA BROKER
start rmiregistry 32000 
java -Djava.rmi.server.hostname=localhost BrokerImpl

# MÁQUINA SERVIDOR Usuarios
start rmiregistry 32001 
java -Djava.rmi.server.hostname=localhost ServidorUsuarios

# MÁQUINA SERVIDOR B
start rmiregistry 32002 
java -Djava.rmi.server.hostname=localhost ServidorMensajes

# MÁQUINA CLIENTE
java Cliente


# Primero debo de encender las máquinas remotas
ssh a869800@central.cps.unizar.es

# Encender máquina 197 (Servidores)
/usr/local/etc/wake -y lab102-196
/usr/local/etc/wake -y lab102-197
/usr/local/etc/wake -y lab102-198


# Verificar que responden
ping 155.210.154.196
ping 155.210.154.197
ping 155.210.154.198

# EJECUCIÓN SOBRE LAS MÁQUINAS DEL LABORATORIO
# 1. Conectarte por SSH a la máquina del broker
ssh a869800@155.210.154.196

# 2. Copiar todos los archivos .java a esa máquina
scp *.java a869800@155.210.154.196:~/practica_broker/

# Ir al directorio
cd ~/practica_broker

# 3. Compilar en la máquina remota
javac *.java

# Verificar si ya hay un rmiregistry corriendo
ps aux | grep rmiregistry

# Si hay alguno, matarlo
kill -9 PID_del_proceso

# 4. Lanzar rmiregistry en esa máquina
rmiregistry 32000 &
rmiregistry 32001 & 
rmiregistry 32002 &

# 5. Lanzar el Broker en esa máquina
java -Djava.rmi.server.hostname=155.210.154.196 BrokerImpl
java -Djava.rmi.server.hostname=155.210.154.197 ServidorMensajes & 
java -Djava.rmi.server.hostname=155.210.154.197 ServidorUsuarios &
El cliente simplemente compilo: javac Cliente.java, y lo lanzo java Cliente
# En el 96->Broker, 97->Servers. 98->Cliente