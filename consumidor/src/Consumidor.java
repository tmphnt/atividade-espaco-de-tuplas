import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.discovery.LookupLocator;
import net.jini.space.JavaSpace;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Retira e processa tarefas do espaço de tuplas via take() (equivalente a IN no modelo Linda).
 *
 * Comportamento controlado por config.properties:
 *   buscar_alta_prioridade_primeiro=false  → retira qualquer tarefa disponível
 *   buscar_alta_prioridade_primeiro=true   → tenta prioridade 1 primeiro; se não encontrar
 *                                            em 2s, aceita qualquer prioridade
 *
 * O consumidor não conhece o produtor. Não há qualquer referência ao Produtor.java aqui.
 */
public class Consumidor {

    private static final String REGGIE_HOST = "reggie";
    private static final long TIMEOUT_ALTA_PRIO = 2000; // ms de espera por tarefa de alta prioridade

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.policy", "security.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        boolean altaPrioridadePrimeiro = carregarConfig();
        String nome = System.getenv().getOrDefault("NOME_CONSUMIDOR", "CONSUMIDOR");

        JavaSpace space = descobrirEspaco(nome);
        System.out.println("[" + nome + "] Espaço encontrado. Aguardando tarefas...");

        while (true) {
            TaskEntry tarefa = buscarTarefa(space, altaPrioridadePrimeiro);
            System.out.println("[" + nome + "] take: " + tarefa);
            Thread.sleep(1000); // simula processamento
            System.out.println("[" + nome + "] Processamento concluído: tarefa " + tarefa.id);
        }
    }

    private static TaskEntry buscarTarefa(JavaSpace space, boolean altaPrioridadePrimeiro)
            throws Exception {
        if (altaPrioridadePrimeiro) {
            // Template para tarefas de alta prioridade (prioridade=1, demais campos = curinga)
            TaskEntry templateAlta = new TaskEntry(null, null, 1);
            TaskEntry tarefa = (TaskEntry) space.take(templateAlta, null, TIMEOUT_ALTA_PRIO);
            if (tarefa != null) {
                return tarefa;
            }
            // Não encontrou alta prioridade no timeout — aceita qualquer tarefa
            System.out.println("[" + System.getenv().getOrDefault("NOME_CONSUMIDOR", "CONSUMIDOR")
                + "] Nenhuma tarefa de alta prioridade disponível. Buscando qualquer tarefa...");
        }
        // Template sem restrição de prioridade: todos os campos são curinga
        TaskEntry templateQualquer = new TaskEntry(null, null, null);
        return (TaskEntry) space.take(templateQualquer, null, Long.MAX_VALUE);
    }

    private static boolean carregarConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (Exception e) {
            // arquivo ausente: usa padrão
        }
        return Boolean.parseBoolean(props.getProperty("buscar_alta_prioridade_primeiro", "false"));
    }

    private static JavaSpace descobrirEspaco(String nome) throws Exception {
        LookupLocator locator = new LookupLocator("jini://" + REGGIE_HOST);
        ServiceTemplate template = new ServiceTemplate(null, new Class[]{JavaSpace.class}, null);

        for (int tentativa = 1; tentativa <= 20; tentativa++) {
            try {
                ServiceRegistrar registrar = locator.getRegistrar();
                Object service = registrar.lookup(template);
                if (service != null) {
                    return (JavaSpace) service;
                }
            } catch (Exception e) {
                // reggie ou javaspaces ainda não prontos
            }
            System.out.println("[" + nome + "] Aguardando espaço... (" + tentativa + "/20)");
            Thread.sleep(2000);
        }
        throw new RuntimeException("[" + nome + "] Não foi possível encontrar o espaço de tuplas.");
    }
}
