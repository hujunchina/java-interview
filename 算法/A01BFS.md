2020年5月12日 10:27:16

### 目录

1. 概念
2. 原理
3. 算法
4. 应用



### 1. 概念

BFS 广度优先搜索，广度就是横向扩展，对于树的层次就是广度，先搜索树的每一层再搜索下一层。

这样的搜索特性是通过队列来实现的，因为有了队列可以保存一些节点而后用。

### 2. 原理

BFS 利用队列，通过让节点不断的入队出队来实现一层一层的遍历。

原本的数据结构是不具备层次的，即无法通过数据结构本身的变量访问该层次的左右节点，所以要把上个节点保存到队列中，既可以延迟访问该节点，也可以不丢失该节点的子节点。

### 3. 算法

```java
public class Main{
    public ArrayList<Integer> bfs(int point){
        ArrayList<Integer> res = new ArrayList<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(arr[1]);
        while(!queue.isEmpty()){
            int tmp = queue.poll();
            res.add(tmp);
            queue.offer(tmp.left);
            queue.offer(tmp.right);
        }
        return res;
    }
}
```

Java 中使用 LInkedList 实现队列，但声明类型还是 Queue，入队用 offer，出队用 poll，查看用 peek。

### 4. 应用

- 树的层次遍历
- 对图遍历
- 走迷宫等