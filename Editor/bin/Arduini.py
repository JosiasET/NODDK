nombre = "Ana"
edad = 22
param "Hola"
param nombre
t0 = print call 2
param "Tu edad es"
param edad
t1 = println call 2
t2 = edad >= 18
ifFalse t2 goto L0
param "Eres mayor de edad"
t3 = println call 1
goto L1
L0:
param "Eres menor de edad"
t4 = println call 1
L1:
contador = 0
L2:
t5 = contador < 3
ifFalse t5 goto L3
param "Contador actual:"
param contador
t6 = println call 2
t7 = contador + 1
contador = t7
goto L2
L3:
param "Programa terminado"
t8 = print call 1