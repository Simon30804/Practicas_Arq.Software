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

