// Código generado para ESP32 (Arduino Framework)
#include <Arduino.h>

#include <vector>
// Pila de operandos para llamadas a función
std::vector<double> _stack;

// Variables Variables
double t4, a, t5, b, t6, t10, t7, val3, t11, val2, val1;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("--- INICIO ---");
}

void loop() {
  a = 10;
  b = 20;
  val1 = 205;
  val2 = 205;
  _stack.push_back("Val1: ");
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  _stack.erase(_stack.end() - 1, _stack.end());
  _stack.push_back(205);
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 1, _stack.end());
  _stack.push_back("Val2: ");
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  _stack.erase(_stack.end() - 1, _stack.end());
  _stack.push_back(205);
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 1, _stack.end());
  a = 30;
  val3 = 605;
  _stack.push_back("Val3: ");
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  _stack.erase(_stack.end() - 1, _stack.end());
  _stack.push_back(605);
  for(int i=0; i<1; i++) {
    Serial.print(_stack[_stack.size() - 1 + i]);
    if(i < 0) Serial.print(" ");
  }
  Serial.println();
  _stack.erase(_stack.end() - 1, _stack.end());

  // Fin del programa
  while(1) { delay(100); }
}
