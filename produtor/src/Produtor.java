import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.discovery.LookupLocator;
import net.jini.space.JavaSpace;

/**
 * Deposita tarefas no espaço de tuplas via write() (equivalente a OUT no modelo Linda).
 *
 * O produtor não conhece nenhum consumidor — descobre o espaço via reggie
 * e escreve tuplas sem qualquer referência a quem vai processá-las.
 */
public class Produtor {

    private static final String REGGIE_HOST = "reggie";
    private static final long LEASE_DURATION = 60 * 60 * 1000; // 1 hora em ms

    // Tarefas a depositar: (id, tipo, prioridade)
    // prioridade 1 = alta, 2 = baixa
    private static final Object[][] TAREFAS = {
        {1, "calcular", 2},
        {2, "calcular", 1},
        {3, "calcular", 2},
        {4, "calcular", 1},
        {5, "calcular", 1},
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.policy", "security.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        JavaSpace space = descobrirEspaco();
        System.out.println("[PRODUTOR] Espaço encontrado via lookup.");

        for (Object[] t : TAREFAS) {
            TaskEntry entry = new TaskEntry((Integer) t[0], (String) t[1], (Integer) t[2]);
            space.write(entry, null, LEASE_DURATION);
            System.out.println("[PRODUTOR] write: " + entry);
            Thread.sleep(800);
        }

        System.out.println("[PRODUTOR] Todas as tarefas depositadas. Encerrando.");
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
            System.out.println("[PRODUTOR] Aguardando espaço... (" + tentativa + "/20)");
            Thread.sleep(2000);
        }
        throw new RuntimeException("[PRODUTOR] Não foi possível encontrar o espaço de tuplas.");
    }
}
