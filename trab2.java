/*  
**  Disciplina: Computacao Concorrente 
**  Envolvidos: Lucca Ricardo e Júlia Pohlmann 
**  Trabalho 2 
**  Codigo: Monitoramento de temperatura no padrão leitores e escritores com prioridade para escrita usando threads em Java 
*/

import java.util.*;

//--PASSO 1: criar uma classe com padrão leitores/escritores, com prioridade para escrita 
class Temperature {
   public Queue<Double> lastReadings = new Queue<Double>();
   public Double avgTemperature; 
   
   //--construtor
   public Temperature() { 
      //
   }

   //--devolve temperatura no intervalo [25,40]
   private double getTemperature() {
      temperature = ((Math.random() * 15) + 25);
      this.updateLastReadings(temperature);
      return temperature;
   }

   private void updateLastReadings(double temperature){
      if( this.lastReadings.length >= 15)
         this.lastReadings.poll();
      this.lastReadings.add(temperature);
   }

}

//--classe do metodo main
class HelloThread {
   public static void main (String[] args) {

      //--recebe e valida os parâmetros passados
      if(args.length < 1){
         System.out.println("Executar: arquivo <Num. sensores>" + args.length);
         return;
      }
      int nsensores  = Integer.parseInt(args[0]);

      //--reserva espaço para um vetor de threads
      Thread[] threads = new Thread[nsensores];

      //--PASSO 3: iniciar a thread
      for (int i=0; i<threads.length; i++) {
         threads[i].start();
      }

      //--PASSO 4: esperar pelo termino das threads (sem esse passo a main pode terminar antes das threads)
      for (int i=0; i<threads.length; i++) {
            try { threads[i].join(); } 
            catch (InterruptedException e) { return; }
      }

      System.out.println("Terminou"); 
   }
}
