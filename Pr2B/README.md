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


# Primero debemos de encender las máquinas remotas, para ello nos conectamos a central:
ssh a869800@central.cps.unizar.es

# Encendemos las máquinas 196, 197 y 198, donde lanzaremos el Broker, los Servidores y el Cliente
/usr/local/etc/wake -y lab102-196
/usr/local/etc/wake -y lab102-197
/usr/local/etc/wake -y lab102-198


# Verificamos que responden
ping 155.210.154.196
ping 155.210.154.197
ping 155.210.154.198

# EJECUCIÓN SOBRE LAS MÁQUINAS DEL LABORATORIO
# 1. Nos conectamos por SSH a la máquina del Broker, de los Servidores y del Cliente
ssh a869800@155.210.154.196
ssh a869800@155.210.154.197
ssh a869800@155.210.154.198

# 2. Copiamos todos los archivos .java en la máquina
scp *.java a869800@155.210.154.196:~/practica_broker/
scp *.java a869800@155.210.154.197:~/practica_broker/
scp *.java a869800@155.210.154.198:~/practica_broker/

# Vamos al directorio
cd ~/practica_broker

# 3. Compilamos los archivos en la máquina remota
javac *.java

# Verificamos si ya hay un rmiregistry corriendo
ps aux | grep rmiregistry

# Si hay alguno, matarlo
kill -9 PID_del_proceso

# 4. Lanzamos rmiregistry en cada máquina
rmiregistry 32000 & (96)
rmiregistry 32001 & (97)
rmiregistry 32002 & (97)

# 5. Lanzamos el Broker, los Servidores y los Clientes 
java -Djava.rmi.server.hostname=155.210.154.196 BrokerImpl
java -Djava.rmi.server.hostname=155.210.154.197 ServidorMensajes  
java -Djava.rmi.server.hostname=155.210.154.197 ServidorUsuarios 
El cliente síncrono simplemente compilo: javac Cliente.java y lo lanzo con: java Cliente
El cliente asíncrono simplemente compilo: javac ClienteAsincrono.java y lo lanzo con: java ClienteAsincrono
# En el 96->Broker, 97->Servers. 98->Cliente

# Comprobamos que los servicios se registran de manera dinámica en el Broker, de manera que solo necesitamos recompilar y ejecutar de nuevo el servidor, tanto el Broker, como el Cliente pueden mantener su ejecución 
Primero verificamos si el rmiregistry corriendo: ps aux | grep rmiregistry
Matamos el rmiregistry correspondiente al ServidorMensajes, (el que lanzado en 32001): kill -9 PID_del_proceso
Damos de baja uno de los servicios registrados en el ServidorMensajes (Descomentando la linea 157 comentada)
