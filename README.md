# Distributed-System-Design

## Terminal 1 runs registry
```
javac *.java
rmic RemoteImplementation
rmiregistry
```

## Terminal 2 Run server 1
```
java Atwater
```

## Terminal 3 Run server 2
```
java Verdun
```

## Terminal 4 Run server 3
```
java Outrement
```

## Terminal 5 Run client
```
java Client
```