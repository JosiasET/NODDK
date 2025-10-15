package editor;

import java.util.Scanner;

public class Ejemplo {
    public static void main(String[] args) {
        
        Scanner sc = new Scanner(System.in);
        int numero;
        do {
            System.err.println("Numero");
            numero = sc.nextInt(); 

        } while (numero <= 5);

    }
}
