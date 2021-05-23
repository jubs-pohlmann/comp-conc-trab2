/*  
**  Disciplina: Computacao Concorrente 
**  Envolvidos: Lucca Ricardo e Júlia Pohlmann 
**  Trabalho 2 
**  Codigo: Monitoramento de temperatura no padrão leitores e escritores com prioridade para escrita usando threads em Java 
*/

import java.util.*;

class LE {
	private int leit, escr, wantWrite;  
	
	// Construtor
	LE() { 
	   this.leit = 0; //leitores lendo (0 ou mais)
	   this.escr = 0; //escritor escrevendo (0 ou 1)
	   this.wantWrite = 0; // Escritores esperando prioridade
	} 
	
	// Entrada para leitores
	public synchronized void EntraLeitor (int id) {
	  try { 
		while (this.escr > 0 || this.wantWrite > 0) {
		   // System.out.println ("le.leitorBloqueado("+id+")");
		   wait();  //bloqueia pela condicao logica da aplicacao 
		}
		this.leit++;  //registra que ha mais um leitor lendo
		// System.out.println ("le.leitorLendo("+id+")");
	  } catch (InterruptedException e) { }
	}
	
	// Saida para leitores
	public synchronized void SaiLeitor (int id) {
	   this.leit--; //registra que um leitor saiu
	   if (this.leit == 0) 
			 this.notify(); //libera escritor (caso exista escritor bloqueado)
	   // System.out.println ("le.leitorSaindo("+id+")");
	}
	
   public void PriorityWrite (int id) {
      this.wantWrite++;
      this.EntraEscritor(id);
    } 

	// Entrada para escritores
	public synchronized void EntraEscritor (int id) {
	  try { 
		while ((this.leit > 0) || (this.escr > 0)) {
		   // System.out.println ("le.escritorBloqueado("+id+")");
		   wait();  //bloqueia pela condicao logica da aplicacao 
		}
		this.escr++; //registra que ha um escritor escrevendo
		// System.out.println ("le.escritorEscrevendo("+id+")");
	  } catch (InterruptedException e) { }
	}
	
	// Saida para escritores
	public synchronized void SaiEscritor (int id) {
	   this.escr--; //registra que o escritor saiu
      this.wantWrite--; // registra que o conseguiu a prioridade;
	   notifyAll(); //libera leitores e escritores (caso existam leitores ou escritores bloqueados)
	   // System.out.println ("le.escritorSaindo("+id+")");
	}
  }

//--PASSO 1: criar uma classe com padrão leitores/escritores, com prioridade para escrita 
class Sensor extends Thread {
   private int id;
   private LE leitorEscritor;
   private Queue<int[]> lastReadings;
   private int readIndex = 0;

   //--construtor
   public Sensor(int id, LE leitorEscritor, Queue<int[]> lastReadings) { 
      this.id = id;
      this.leitorEscritor = leitorEscritor;
      this.lastReadings = lastReadings;
   }

   //--devolve temperatura no intervalo [25,40]
   private int getTemperature() {
      this.readIndex++;
      return (int) ((Math.random() * 15) + 25);
   }

   public void run(){
      try{
         int temperature;

         while(true){
            temperature = getTemperature();
            System.out.println ("Sensor "+this.id+ " registrou temperatura de "+temperature+"°C");
            if(temperature > 30){
               leitorEscritor.PriorityWrite(this.id);
               int[] tripla = {this.id, temperature, this.readIndex};
               this.lastReadings.add(tripla);
               leitorEscritor.SaiEscritor(this.id);
            }
            sleep(1000);
         }
      }catch (InterruptedException e) { return; }
   }
}

class Atuador extends Thread {
   private int id;
   private LE leitorEscritor;
   private Queue<int[]> lastReadings;
   private String alerta; 

   public Atuador(int id, LE leitorEscritor, Queue<int[]> lastReadings) { 
      this.id = id;
      this.leitorEscritor = leitorEscritor;
      this.lastReadings = lastReadings;
   }

   public void run(){
      try {
         while(true){
            int avg = 0;
            int nLeituras = 0;
            int soma = 0;
            int acima35 = 0;
            this.alerta = null;

            leitorEscritor.EntraLeitor(this.id);
         
            for(int[] t : this.lastReadings){
               if(t[0] != this.id) continue;
               nLeituras++;
               soma += t[1];
               if(t[1] > 35) acima35++;
               if(t[1] > 35 && nLeituras == 5) this.alerta = "Vermelho";
               if(this.alerta == null && acima35 >= 5 && nLeituras == 15) this.alerta = "Amarelo";
            }

            if(this.alerta == null) this.alerta = "Normal";
            if(nLeituras != 0) avg = soma/nLeituras;

            System.out.println("SENSOR "+ this.id + " Alerta: " + this.alerta + " MÉDIA: " + avg+"°C");
            leitorEscritor.SaiLeitor(this.id);
            
            sleep(2000);
            
         }
      } catch (InterruptedException e) {return;}
   }
}

//--classe do metodo main
class MonitorTemperature {
   public static void main (String[] args) {
      LE leitorEscritor = new LE();
      Queue<int[]> lastReadings = new ArrayDeque<int[]>(60);

      //--recebe e valida os parâmetros passados
      if(args.length < 1){
         System.out.println("Executar: arquivo <Num. sensores>");
         return;
      }

      int nsensores  = Integer.parseInt(args[0]);

      //--reserva espaço para um vetor de threads
      Thread[] sensores = new Thread[nsensores];
      Thread[] atuadores = new Thread[nsensores];
      //--PASSO 3: iniciar a thread
      for (int i=0; i< nsensores; i++) {
         sensores[i] = new Sensor(i, leitorEscritor, lastReadings);
         sensores[i].start();
         atuadores[i] = new Atuador(i, leitorEscritor, lastReadings);
         atuadores[i].start();
      }

      //--PASSO 4: esperar pelo termino das threads (sem esse passo a main pode terminar antes das threads)
   //    for (int i=0; i<nsensores; i++) {
   //          try { 
   //             sensores[i].join(); 
   //             atuadores[i].join(); 
   //          } 
   //          catch (InterruptedException e) { return; }
   //    }

   //    System.out.println("Terminou"); 
   // }
}
