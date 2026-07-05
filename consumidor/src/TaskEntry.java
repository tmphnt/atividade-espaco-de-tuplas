import net.jini.core.entry.Entry;

/**
 * Representa uma tarefa no espaço de tuplas.
 *
 * Todos os campos são públicos e não-finais — requisito da interface Entry do Jini.
 * Um campo null em um template de busca funciona como curinga (casa com qualquer valor).
 */
public class TaskEntry implements Entry {

    /** Identificador único da tarefa. */
    public Integer id;

    /** Tipo da tarefa (ex: "calcular"). */
    public String tipo;

    /**
     * Prioridade da tarefa.
     * 1 = alta, 2 = baixa.
     * Um consumidor pode buscar somente tarefas de prioridade alta
     * usando um template com prioridade=1 e os demais campos null.
     */
    public Integer prioridade;

    /** Construtor sem argumentos — obrigatório pela interface Entry. */
    public TaskEntry() {}

    public TaskEntry(Integer id, String tipo, Integer prioridade) {
        this.id = id;
        this.tipo = tipo;
        this.prioridade = prioridade;
    }

    @Override
    public String toString() {
        return "TaskEntry{id=" + id + ", tipo=\"" + tipo + "\", prioridade=" + prioridade + "}";
    }
}
