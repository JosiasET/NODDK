// Código generado para ESP32 (Arduino Framework)
#include <Arduino.h>

#include <vector>
struct Variant {
    double numVal;
    String strVal;
    int type; // 0=num, 1=str

    Variant(double d) : numVal(d), type(0) {}
    Variant(int i) : numVal(i), type(0) {}
    Variant(String s) : strVal(s), type(1) { numVal = 0; }
    Variant(const char* s) : strVal(s), type(1) { numVal = 0; }
};

// Pila de operandos
std::vector<Variant> _stack;

// Variables Globales
double t4, t5, t6, t7, t8, contador, t0, edad, t1, t2, t3;
String nombre;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("--- INICIO ---");
}

void loop() {
  nombre = "Ana";
  edad = 22;
  _stack.push_back(Variant("Hola"));
  _stack.push_back(Variant(nombre));
  for(int i=0; i<2; i++) {
    Variant v = _stack[_stack.size() - 2 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 1) Serial.print(" ");
  }
  _stack.erase(_stack.end() - 2, _stack.end());
  _stack.push_back(Variant("Tu edad es"));
  _stack.push_back(Variant(edad));
  for(int i=0; i<2; i++) {
    Variant v = _stack[_stack.size() - 2 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 1) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 2, _stack.end());
  t2 = (edad >= 18);
  if (!t2) goto L0;
  _stack.push_back(Variant("Eres mayor de edad"));
  for(int i=0; i<1; i++) {
    Variant v = _stack[_stack.size() - 1 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 0) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 1, _stack.end());
  goto L1;
L0:
  _stack.push_back(Variant("Eres menor de edad"));
  for(int i=0; i<1; i++) {
    Variant v = _stack[_stack.size() - 1 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 0) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 1, _stack.end());
L1:
  contador = 0;
L2:
  t5 = (contador < 3);
  if (!t5) goto L3;
  _stack.push_back(Variant("Contador actual:"));
  _stack.push_back(Variant(contador));
  for(int i=0; i<2; i++) {
    Variant v = _stack[_stack.size() - 2 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 1) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 2, _stack.end());
  t7 = contador + 1;
  contador = t7;
  goto L2;
L3:
  _stack.push_back(Variant("Programa terminado"));
  for(int i=0; i<1; i++) {
    Variant v = _stack[_stack.size() - 1 + i];
    if(v.type == 1) Serial.print(v.strVal);
    else Serial.print(v.numVal);
    if(i < 0) Serial.print(" ");
  }
  _stack.erase(_stack.end() - 1, _stack.end());

  // Fin del programa
  while(1) { delay(100); }
}
