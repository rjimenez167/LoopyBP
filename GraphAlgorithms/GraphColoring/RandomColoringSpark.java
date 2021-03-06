package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.spark.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

//import com.tinkerpop.blueprints.*;
//import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class RandomColoringSpark {

	// Colored Map: <colorStateNumber,summary>
	public static final Map<Integer, ArrayList<Integer>> coloredMap = new HashMap<Integer, ArrayList<Integer>>();
	// The global graph variable
	public static Graph graph = null;
	public static int graphSize = 0;
	public static SparkGraphComputer spark = null;
	public static HadoopGraph hadoopGraph = null;
	
	public static void main(String[] args) throws IOException {

		// Load in the graph
		graph =  TinkerGraph.open();
		
		// Need to copy?
		hadoopGraph = HadoopGraph.open();

		// I loaded from a csv file
		String filename = "data/test0_3_58_1355.csv";
		load(filename);
		
		
		// show a brief info of the graph
		System.out.println("\nGraph:" + graph.toString() + "\n"+coloredMap.toString());	
		
		// Do Jones-Plassmann Graph Coloring
		doRandomColoring();
//		show(graph);
		
		// show a brief info of the graph
		System.out.println("\nGraph:" + graph.toString() + "\n"+coloredMap.toString());
		System.out.println(coloredMap.size());


	}

	
	public static void load(String filename) {

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					filename)));
			String line;
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, ", \t\n\r\f");
				if (st.countTokens() != 5) {
					System.out.println("Error reading from file \"" + filename
							+ "\"");
					System.exit(1);
				}

				Integer v1Id = Integer.valueOf(st.nextToken());
				Double v1Att = Double.valueOf(st.nextToken());
				Integer v2Id = Integer.valueOf(st.nextToken());
				Double v2Att = Double.valueOf(st.nextToken());
				Double edgeAtt = Double.valueOf(st.nextToken());
				// Read in ids, attributes
				readInVertex(v1Id, v2Id, v1Att, v2Att, edgeAtt);

			}

			graphSize=getGraphSize();
			System.out.print("Read File Done!");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

	}

	// The readin methods, to add vertices and edges to the global graph.
	public static void readInVertex(int id1, int id2, double prop1,
			double prop2, double propEdge) {
		Vertex vertex1;
		Vertex vertex2;
		// Read in vertex 1
		if(graph.vertices(id1).hasNext()){
			vertex1 = graph.vertices(id1).next();
		}else{
			vertex1 = graph.addVertex(T.label, "vertex", T.id, id1, "weight",prop1, "colored",0);
		}
		
		

		if(graph.vertices(id2).hasNext()){
			vertex2 = graph.vertices(id2).next();
		}else{
			
			vertex2 = graph.addVertex(T.label, "vertex", T.id, id2, "weight",prop2,"colored",0);

		}

		// add the Edge
//		graph.edge(null,vertex1,vertex2, "edge");
		vertex1.addEdge("edge", vertex2, "properties", propEdge);

	}

	// Now starts JP algo
	public static void doRandomColoring() {
		// starts from the first color
		int randomColor = 1;
		int sum=0;
		ArrayList<Integer> initRandom = new ArrayList<Integer>();
		initRandom = getRandomVertices(randomColor, 40,initRandom);
//		System.out.println("InitRandom "+initRandom.toString());
		
		// if graph is not empty
		while ((sum<graphSize)){
			// do coloring in a group
			
//			System.out.println("sum "+sum);
			randomColor++;
			sum+=initRandom.size();

			initRandom = getRandomVertices(randomColor, 40,initRandom);

		}
		
//		System.out.println("Cut!");
	}

	// Randomly get a group of vertices by ID
	// If start is false, that means should get neighbors.
	// Only the first time, randomly get IDs
	public static ArrayList<Integer> getRandomVertices(int colorNumber,
			int number,ArrayList<Integer> initArrayList) {

		ArrayList<Integer> chosen = new ArrayList<Integer>();
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		Random random=new Random();
		//if initialized,Id should be all the vertices
		if(initArrayList.size()==0){
			Iterator<Vertex> vertexItorIterator = graph.vertices();
			
			while (vertexItorIterator.hasNext()) {
				Vertex vertex=vertexItorIterator.next();
				int id=Integer.parseInt(vertex.id().toString());
				list.add(id);
			}
			
			int delt=0;

			while (delt < 40) {

				int index = random.nextInt(list.size() - 1);
//				System.out.println("Index " + index+"\tdelt "+delt+"\tlist"+list.size());
				int id = list.get(index);
				if (!chosen.contains(id)) {
					chosen.add(id);
					delt++;
				}
			}	
		}
		
		else{
			// get neighours of init arraylist
//			System.out.println("Second Time\n"+initArrayList.toString());
			// assign neighbors
			for (int m = 0; m < initArrayList.size(); m++) {
				Vertex vertex = graph.vertices(initArrayList.get(m)).next();
//				System.out.println("Current "+vertex.getId());
				// Get adjacent vertices
				Iterator<Vertex> ajacent = vertex.vertices(Direction.OUT);
				

				while (ajacent.hasNext()) {
					Vertex neighbour = ajacent.next();
					int newId = Integer.parseInt(neighbour.id().toString());
//					System.out.println("Nei: "+newId); 
//					System.out.println("Color:" + isColored(neighbour)+";");
					if ((!isColored(neighbour)) && (!list.contains(newId))) {
						list.add(newId);
					}
				}
			}
			
			
			int delt=0;

			while (delt < (list.size()/3)) {

				int index = random.nextInt(list.size() - 1);
//				System.out.println("Index " + index+"\tdelt "+delt+"\tlist"+list.size());
				int id = list.get(index);
				if (!chosen.contains(id)) {
					chosen.add(id);
					delt++;
				}
			}	

		}
		
//		System.out.println("-- Chosen size "+chosen.size());

			
			
		//delete neighbours
//		System.out.println("Chosen size "+chosen.size());
		for(int i=0;i<chosen.size();i++){
			
			int currentId = chosen.get(i);
			// get neighours
			Vertex vertex = graph.vertices(currentId).next();
			ArrayList<Integer> neighbours = new ArrayList<Integer>();
			Iterator<Vertex> ajacent = vertex.vertices(Direction.OUT);
//			System.out.println("----");
			//get a neighbour list
			while (ajacent.hasNext()) {
				Vertex neighbour=ajacent.next();
				neighbours.add(Integer.parseInt(neighbour.id().toString()));
			}

			// check existance
			for (int j = 0; j < neighbours.size(); j++) {
				
				for (int m = 0; m < chosen.size(); m++) {
					if (chosen.get(m).equals(neighbours.get(j))) {
//						System.out.println("Removed a neighbor "+chosen.get(m));
						chosen.remove(m);
						break;
					}
				}

			}
		}
		
		
		//Parallel Coloring----Do Gibbs Sampling in feach vertex
		for(int i=0;i<chosen.size();i++){
			Vertex vertex = graph.vertices(chosen.get(i)).next();
		
			doColor(vertex);
			System.err.println("Color vertex "+vertex.id()+"\tColor"+vertex.property("colored").value());

		}
//		System.out.println("Chosen "+chosen.size());

		
		
		return chosen;
	}

	public static boolean isColored(Vertex vertex) {

		if (vertex.property("colored").value().equals(0)) {
			return true;
		}
		return false;
	}

	public static void doColor(Vertex vertex) {
		
		//get min
		int min=1;
		int color=1;
		
		
//		vertex.setProperty("colored", 4);
		
		if(coloredMap.size()==0){
//			color=1;
			ArrayList<Integer> array=new ArrayList<Integer>();
			array.add(Integer.parseInt(vertex.id().toString()));
			coloredMap.put(color, array);
//			System.out.println("Init....");
		
		}else{
			
			//find neighbour colors
			Iterator<Vertex> ajacent = vertex.vertices(Direction.OUT);
			Set<Integer> neighbours = new HashSet();
			
			while (ajacent.hasNext()) {
				Vertex neighbour=ajacent.next();
				if(isColored(neighbour)){
					int state = Integer.parseInt((neighbour.property("colored").value().toString()));
					neighbours.add(state);
				}
			}
			
			
				
			if(neighbours.size()==coloredMap.size()){
				//add a new color
				color=coloredMap.size()+1;
//				System.out.println("--------- color New: "+(neighbours.size()+1));
//				System.out.println("Neighbours colors "+neighbours.toString());

				ArrayList<Integer> array=new ArrayList<Integer>();
				array.add(Integer.parseInt(vertex.id().toString()));
				
				coloredMap.put(coloredMap.size()+1, array);
//				color=coloredMap.size()+1;
			}else{
				//find min
				int key= getMinumColor();
				ArrayList<Integer> array = coloredMap.get(key);
				array.add(Integer.parseInt(vertex.id().toString()));
//				System.out.println("--------- color Old: "+ key);
//				System.out.println("Neighbours colors "+neighbours.toString());
				
				coloredMap.put(key,array);

				color=key;
			}
			
		}
		
		
		vertex.property(VertexProperty.Cardinality.single,"colored",color);
		

	}
	
	
	public static Integer getMinumColor(){
		int min=coloredMap.get(1).size();
		int key=1;
		
		if(coloredMap.size()!=0){
			for(Entry<Integer, ArrayList<Integer>> entry : coloredMap.entrySet()){
				if(entry.getValue().size()<min){
					min=entry.getValue().size();
					key=entry.getKey();
				}
			}
		}
		return key;
	}
	
	public static Boolean allColored(){

		Iterator<Vertex> vertexItorIterator = graph.vertices();
		
		while (vertexItorIterator.hasNext()) {
			Vertex vertex=vertexItorIterator.next();
			System.out.println("IF? "+vertex.property("colored").value());
			if((vertex.property("colored").value()).equals(0))
				return false;
		
		}
		return true;
		
		
		
	}
	
	public static int getGraphSize(){
		int size=0;
		// get size of the graph
		Iterator<Vertex> vertexItorIterator = graph.vertices();

		while (vertexItorIterator.hasNext()) {
			Vertex vertex = vertexItorIterator.next();
			size++;
		}
		return size;
	}
	public static void show(Graph graph){
		Iterator<Vertex> vertexItorIterator = graph.vertices();
		
		while (vertexItorIterator.hasNext()) {
			Vertex vertex=vertexItorIterator.next();
			System.out.println(" " + vertex.id()+"\t"+vertex.property("colored").value());
		
		}
		
	}

}

