import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.discovery.LookupLocator;
import net.jini.space.JavaSpace;

/**
 * Observa o espaço de tuplas a cada 3 segundos e exibe quantas tarefas pendentes existem.
 *
 * O monitor NÃO deve remover tarefas do espaço — apenas observar.
 *
 * Nível 2B: localize o comentário TODO abaixo e complete a implementação.
 */
public class Monitor {

    private static final String REGGIE_HOST = "reggie";
    private static final int INTERVALO_SEGUNDOS = 3;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.policy", "security.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        JavaSpace space = descobrirEspaco();
        System.out.println("[MONITOR] Espaço encontrado. Iniciando monitoramento a cada "
            + INTERVALO_SEGUNDOS + "s...");

        while (true) {
            Thread.sleep(INTERVALO_SEGUNDOS * 1000);
            int pendentes = contarTarefas(space);
            System.out.println("[MONITOR] Tarefas pendentes no espaço: " + pendentes);
        }
    }

    private static int contarTarefas(JavaSpace space) throws Exception {
        TaskEntry template = new TaskEntry(null, null, null);
        int count = 0;

        // Nível 2B: usamos read() e não take(). O take() removeria a tupla do espaço,
        // ou seja, o monitor estaria roubando as tarefas dos consumidores. O read() é
        // não-destrutivo, só olha a tupla e deixa ela onde está.
        //
        // Como o espaço não tem count() e o read() com NO_WAIT pode devolver a mesma
        // tupla mais de uma vez (o Outrigger não garante ordem nem variedade), a gente
        // lê em loop com o template curinga e guarda os id já vistos num conjunto.
        // Paramos depois de algumas leituras seguidas sem id novo, e o total é o tamanho
        // do conjunto. Não é uma contagem exata, é uma heurística, e é justamente a
        // limitação que o Nível 2B quer mostrar.
        java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
        int leiturasSemNovidade = 0;
        final int MAX_SEM_NOVIDADE = 10;

        while (leiturasSemNovidade < MAX_SEM_NOVIDADE) {
            TaskEntry encontrada = (TaskEntry) space.read(template, null, JavaSpace.NO_WAIT);
            if (encontrada == null) {
                // espaço vazio (nenhuma tupla casa com o template): não tem o que contar
                break;
            }
            if (idsVistos.add(encontrada.id)) {
                leiturasSemNovidade = 0;   // achou um id novo, reseta o contador
            } else {
                leiturasSemNovidade++;     // id repetido, o read devolveu algo que ja tinhamos visto
            }
        }

        count = idsVistos.size();
        return count;
    }

    private static JavaSpace descobrirEspaco() throws Exception {
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
            System.out.println("[MONITOR] Aguardando espaço... (" + tentativa + "/20)");
            Thread.sleep(2000);
        }
        throw new RuntimeException("[MONITOR] Não foi possível encontrar o espaço de tuplas.");
    }
}
