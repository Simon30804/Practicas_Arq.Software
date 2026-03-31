# Instrucciones de como comprobar el correcto funcionamiento del sistema desarrollado
## Primero debemos de encender las máquinas remotas, para ello nos conectamos a central:
ssh a869800@central.cps.unizar.es

## Encendemos las máquinas 196, 197 y 198, donde lanzaremos el Broker, los Servidores y el Cliente
/usr/local/etc/wake -y lab102-196
/usr/local/etc/wake -y lab102-197
/usr/local/etc/wake -y lab102-198


## Verificamos que responden
ping 155.210.154.196
ping 155.210.154.197
ping 155.210.154.198

## EJECUCIÓN SOBRE LAS MÁQUINAS DEL LABORATORIO
## 1. Nos conectamos por SSH a la máquina del Broker, de los Servidores y del Cliente
ssh a869800@155.210.154.196
ssh a869800@155.210.154.197
ssh a869800@155.210.154.198

## 2. Copiamos todos los archivos .java en la máquina
scp *.java a869800@155.210.154.196:~/practica_broker/
scp *.java a869800@155.210.154.197:~/practica_broker/
scp *.java a869800@155.210.154.198:~/practica_broker/

## Vamos al directorio
cd ~/practica_broker

## 3. Compilamos los archivos en la máquina remota
javac *.java

## Verificamos si ya hay un rmiregistry corriendo
ps aux | grep rmiregistry

## Si hay alguno, matarlo
kill -9 PID_del_proceso

## 4. Lanzamos rmiregistry en cada máquina
rmiregistry 32000 & (96)
rmiregistry 32001 & (97)
rmiregistry 32002 & (97)

## 5. Lanzamos el Broker, los Servidores y los Clientes 
- <b>java -Djava.rmi.server.hostname=155.210.154.196 BrokerImpl</b>
- <b>java -Djava.rmi.server.hostname=155.210.154.197 ServidorMensajes</b> (Podemos dar de alta/baja una serie de servicios predefinidos de manera dinámica a través de un menu interactivo)
- <b>java -Djava.rmi.server.hostname=155.210.154.197 ServidorUsuarios</b> (Podemos dar de alta/baja una serie de servicios predefinidos de manera dinámica a través de un menu interactivo)
- El cliente síncrono simplemente compilo: <b>javac Cliente.java</b> y lo lanzo con: <b>java Cliente</b>. Este Cliente nos permite hacer uso del sistema desarrollado, a través de un menu de interacción 
- El cliente asíncrono simplemente compilo: <b>javac ClienteAsincrono.java</b> y lo lanzo con: <b>java ClienteAsincrono</b>
## En el 96->Broker, 97->Servers (ServidorMensajes/ServidorUsuarios). 98->Cliente

## Comprobamos que los servicios se registran de manera dinámica en el Broker, tanto el Broker, como el Cliente pueden mantener su ejecución 
Para ello hacemos uso de los menus interactivos disponibles en los servidores (ServidorMensajes, ServidorUsuarios), los cuáles nos permiten dar de alta y de baja servicios, de manera que tanto el Cliente como el Borker pueden mantener su ejecución.

## Nota:
El archivo <b>ClienteAsincrono.java</b> nos permite comprobar el correcto funcionamiento de la comunicación de pruebas por medio de una serie de pruebas.
