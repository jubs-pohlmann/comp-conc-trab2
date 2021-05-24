/*  
**  Disciplina: Computacao Concorrente 
**  Envolvidos: Lucca Ricardo e Júlia Pohlmann 
**  Trabalho 2 
**  Codigo: Monitoramento de temperatura no padrão leitores e escritores com prioridade para escrita usando threads em Java 
*/

import java.util.*;

// Classe para FIFO de numero de elementos fixo
class LimitedSizeQueue<K> extends ArrayList<K> {

   private int maxSize;

   public LimitedSizeQueue(int size){
       this.maxSize = size;
   }

   public boolean add(K k){
       boolean r = super.add(k);
       if (size() > maxSize){
           removeRange(0, size() - maxSize); // Caso tente adicionar mais que o limite, exclui os mais antigos
       }
       return r;
   }

   public K getYoungest() {
       return get(size() - 1);
   }

   public K getOldest() {
       return get(0);
   }
}

class LE {
	private int leit, escr, wantWrite;  
	
	/**
    * 
    * Inicializa a classe LE
    */
	LE() { 
	   this.leit = 0; //leitores lendo (0 ou mais)
	   this.escr = 0; //escritor escrevendo (0 ou 1)
	   this.wantWrite = 0; // Escritores esperando prioridade
	} 
	
	/**
    * 
    * Registra que uma Thread deseja realizar operações de leitura.
    * caso não haja nenhuma Thread Escritora escrevendo ou querendo escrever
    * permite que a operação prossiga.
    * @param id ID da Thread Leitora
    */
	public synchronized void EntraLeitor (int id) {
	  try { 
		while (this.escr > 0 || this.wantWrite > 0) {
         System.out.println("Leitor "+ id +" Bloqueado");
		   wait();  //bloqueia pela condicao logica da aplicacao 
		}
      System.out.println("Leitor "+ id +" prosseguindo");
		this.leit++;  //registra que ha mais um leitor lendo
	  } catch (InterruptedException e) { }
	}
	
	/**
    * 
    * Registra que um leitor terminou suas operações e desperta as threads que estavam a espera.
    * @param id ID da Thread leitora.
    */
	public synchronized void SaiLeitor (int id) {
	   this.leit--; //registra que um leitor saiu
	   if (this.leit == 0) 
			notifyAll();
	}
	
   /**
    * 
    * Registra que uma Thread deseja escrever e define a prioridade da leitura.
    * @param id ID da Thread Escritora
    */
   public void PriorityWrite (int id) {
      this.wantWrite++;
      this.EntraEscritor(id);
    } 

    /**
     * 
     * Permite as operações de escrita caso não haja outros escritores
     * ou leitores operando sobre a lista.
     * @param id ID da Thread Escritora
     */
    public synchronized void EntraEscritor (int id) {
	  try { 
		while ((this.leit > 0) || (this.escr > 0)) {
         System.out.println("Escritor "+ id +" BLOQUEADO*************");
		   wait();  //bloqueia pela condicao logica da aplicacao 
		}
      System.out.println("Escritor "+ id +" prosseguindo");
		this.escr++; //registra que ha um escritor escrevendo
	  } catch (InterruptedException e) { }
	}
	
	/**
    * 
    * Registra que as operações de escrita foram realizadas e acorda
    * as Threads adormecidas.
    * @param id ID da Thread Escritora
    */
	public synchronized void SaiEscritor (int id) {
	   this.escr--; // registra que o escritor saiu
      this.wantWrite--; // registra que o conseguiu a prioridade;
	   notifyAll(); // libera leitores e escritores (caso existam leitores ou escritores bloqueados)
	}
}

//--PASSO 1: criar uma classe com padrão leitores/escritores, com prioridade para escrita 
class Sensor extends Thread {
   private int id;
   private LE leitorEscritor;
   private LimitedSizeQueue<int[]> lastReadings;
   private int readIndex = 0;

   /**
    * 
    * @param id ID da Thread Sensor
    * @param leitorEscritor Instância controladora das Leituras e Escritas
    * @param lastReadings Lista das ultimas leituras registradas por todas as Threads Sensores
    */
   public Sensor(int id, LE leitorEscritor, LimitedSizeQueue<int[]> lastReadings) { 
      this.id = id;
      this.leitorEscritor = leitorEscritor;
      this.lastReadings = lastReadings;
   }

   /**
    * 
    * @return Retorna um numero inteiro aletório entre 25 e 40 
    */
   private int getTemperature() {
      this.readIndex++;
      return (int) ((Math.random() * 15) + 25);
   }

   public void run(){
      try{
         int temperature;

         while(true){
            temperature = getTemperature();
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
   private LimitedSizeQueue<int[]> lastReadings;
   private String alerta; 

   /**
    * 
    * @param id ID da Thread Atuadora
    * @param leitorEscritor Instância controladora das Leituras e Escritas
    * @param lastReadings Lista das ultimas leituras registradas por todas as Threads Sensores
    */
   public Atuador(int id, LE leitorEscritor, LimitedSizeQueue<int[]> lastReadings) { 
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
            ArrayList<Integer> sensorUltimas = new ArrayList<Integer>();
            leitorEscritor.EntraLeitor(this.id);
            
            for(int[] t : this.lastReadings){
               if(t[0] != this.id) continue;
               sensorUltimas.add(t[1]);
               nLeituras++;
               soma += t[1];
               if(t[1] > 35) acima35++;
               if(t[1] > 35 && nLeituras == 5 && acima35 == 5) this.alerta = "Vermelho";
               if(this.alerta == null && acima35 >= 5 && nLeituras <= 15) this.alerta = "Amarelo";
            }

            if(this.alerta == null) this.alerta = "Normal";
            if(nLeituras != 0) avg = soma/nLeituras;
            System.out.println("-------- SENSOR " + this.id + " --------\n Alerta: " + this.alerta + "\n MÉDIA: " + avg+"°C\n Ultimas Leituras: " + sensorUltimas.toString()+"\n---------------------------------------\n");

            leitorEscritor.SaiLeitor(this.id);
            
            sleep(2000);
            
         }
      } catch (InterruptedException e) {return;}
   }
}

// Classe do metodo main
class MonitorTemperature {
   public static void main (String[] args) {
      LE leitorEscritor = new LE();
      LimitedSizeQueue<int[]> lastReadings = new LimitedSizeQueue<int[]>(60);
     
      //Recebe e valida os parâmetros passados
      if(args.length < 1){
         System.out.println("Executar: arquivo <Num. sensores>");
         return;
      }

      int nsensores  = Integer.parseInt(args[0]);

      // Reserva espaço para um vetor de threads
      Thread[] sensores = new Thread[nsensores];
      Thread[] atuadores = new Thread[nsensores];

      // Iniciar as threads
      for (int i=0; i< nsensores; i++) {
         sensores[i] = new Sensor(i, leitorEscritor, lastReadings);
         sensores[i].start();
         atuadores[i] = new Atuador(i, leitorEscritor, lastReadings);
         atuadores[i].start();
      }
   }
}
