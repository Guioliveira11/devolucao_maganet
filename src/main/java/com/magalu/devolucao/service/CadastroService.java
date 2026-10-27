package com.magalu.devolucao.service;

import com.magalu.devolucao.domain.ContaAcesso;
import com.magalu.devolucao.domain.Funcionario;
import com.magalu.devolucao.domain.ItemComprado;
import com.magalu.devolucao.domain.OngParceira;
import com.magalu.devolucao.domain.Pedido;
import com.magalu.devolucao.domain.Usuario;
import com.magalu.devolucao.dto.CadastroClienteRequest;
import com.magalu.devolucao.dto.CadastroFuncionarioRequest;
import com.magalu.devolucao.dto.CadastroOngRequest;
import com.magalu.devolucao.dto.CadastroOpcoesResponse;
import com.magalu.devolucao.exception.ParametroInvalidoException;
import com.magalu.devolucao.exception.RegraNegocioException;
import com.magalu.devolucao.repository.ContaAcessoRepository;
import com.magalu.devolucao.repository.FuncionarioRepository;
import com.magalu.devolucao.repository.LojaRepository;
import com.magalu.devolucao.repository.OngParceiraRepository;
import com.magalu.devolucao.repository.PedidoRepository;
import com.magalu.devolucao.repository.ProdutoRepository;
import com.magalu.devolucao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cadastro de contas. Cliente: aberto. Funcionario e ONG: exigem um codigo de convite
 * definido no ambiente (CODIGO_CONVITE_FUNCIONARIO / CODIGO_CONVITE_ONG); sem o codigo
 * configurado, esse tipo de cadastro fica desligado.
 */
@Service
@RequiredArgsConstructor
public class CadastroService {

    private static final Pattern LOGIN = Pattern.compile("^[a-z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern MATRICULA = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final BigDecimal[] PRECOS_DEMO = {
            new BigDecimal("149.90"), new BigDecimal("89.90"), new BigDecimal("59.90")};

    private final UsuarioRepository usuarios;
    private final ContaAcessoRepository contas;
    private final FuncionarioRepository funcionarios;
    private final OngParceiraRepository ongs;
    private final LojaRepository lojas;
    private final ProdutoRepository produtos;
    private final PedidoRepository pedidos;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Value("${CODIGO_CONVITE_FUNCIONARIO:}")
    private String conviteFuncionario;

    @Value("${CODIGO_CONVITE_ONG:}")
    private String conviteOng;

    /** Se verdadeiro, todo cliente novo recebe um pedido de demonstracao para testar o fluxo. */
    @Value("${CADASTRO_COMPRAS_DEMO:false}")
    private boolean comprasDemo;

    @Transactional(readOnly = true)
    public CadastroOpcoesResponse opcoes() {
        boolean funcionario = !conviteFuncionario.isBlank();
        boolean ong = !conviteOng.isBlank();
        List<CadastroOpcoesResponse.LojaOpcao> listaLojas = new ArrayList<>();
        if (funcionario) {
            for (var loja : lojas.findByAtivaTrueOrderByNomeAsc())
                listaLojas.add(new CadastroOpcoesResponse.LojaOpcao(loja.getId(), loja.getNome()));
        }
        List<String> categorias = ong ? produtos.findCategorias() : new ArrayList<>();
        return new CadastroOpcoesResponse(funcionario, ong, listaLojas, categorias);
    }

    @Transactional
    public void cadastrarCliente(CadastroClienteRequest r) {
        String nome = textoObrigatorio(r.nome(), "Nome", 150);
        String cpf = Documentos.soDigitos(r.cpf());
        if (!Documentos.cpfValido(cpf)) throw new ParametroInvalidoException("CPF inválido.");
        String email = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (email.length() > 255 || !EMAIL.matcher(email).matches())
            throw new ParametroInvalidoException("E-mail inválido.");
        String telefone = r.telefone() == null || r.telefone().isBlank() ? null : r.telefone().trim();
        if (telefone != null && telefone.length() > 20) throw new ParametroInvalidoException("Telefone muito longo.");
        String login = validarAcesso(r.login(), r.senha());

        if (usuarios.existsByCpf(cpf) || usuarios.existsByEmailIgnoreCase(email))
            throw new RegraNegocioException("Já existe um cadastro com este CPF ou e-mail.");

        var usuario = usuarios.save(Usuario.builder().nome(nome).cpf(cpf).email(email).telefone(telefone).build());
        contas.save(ContaAcesso.builder().login(login).senhaHash(encoder.encode(r.senha())).usuario(usuario).build());
        if (comprasDemo) criarPedidoDemo(usuario);
    }

    @Transactional
    public void cadastrarFuncionario(CadastroFuncionarioRequest r) {
        exigirConvite(conviteFuncionario, r.codigoConvite(), "funcionários");
        String nome = textoObrigatorio(r.nome(), "Nome", 150);
        String matricula = r.matricula() == null ? "" : r.matricula().trim();
        if (!MATRICULA.matcher(matricula).matches())
            throw new ParametroInvalidoException("Matrícula inválida: use de 3 a 50 letras, números, ponto, hífen ou sublinhado.");
        if (r.lojaId() == null) throw new ParametroInvalidoException("Escolha a loja onde você trabalha.");
        var loja = lojas.findById(r.lojaId()).filter(l -> l.isAtiva())
                .orElseThrow(() -> new ParametroInvalidoException("Loja inválida ou inativa."));
        String login = validarAcesso(r.login(), r.senha());

        if (funcionarios.existsByMatricula(matricula))
            throw new RegraNegocioException("Já existe um funcionário com esta matrícula.");

        var funcionario = funcionarios.save(Funcionario.builder().nome(nome).matricula(matricula).loja(loja).build());
        contas.save(ContaAcesso.builder().login(login).senhaHash(encoder.encode(r.senha())).funcionario(funcionario).build());
    }

    @Transactional
    public void cadastrarOng(CadastroOngRequest r) {
        exigirConvite(conviteOng, r.codigoConvite(), "ONGs");
        String nome = textoObrigatorio(r.nome(), "Nome da ONG", 255);
        String cnpj = Documentos.soDigitos(r.cnpj());
        if (!Documentos.cnpjValido(cnpj)) throw new ParametroInvalidoException("CNPJ inválido.");

        var escolhidas = new LinkedHashSet<String>();
        if (r.categorias() != null) r.categorias().stream().filter(c -> c != null && !c.isBlank())
                .map(String::trim).forEach(escolhidas::add);
        if (escolhidas.isEmpty()) throw new ParametroInvalidoException("Escolha ao menos uma categoria de produto.");
        var conhecidas = new HashSet<>(produtos.findCategorias());
        for (String categoria : escolhidas)
            if (!conhecidas.contains(categoria)) throw new ParametroInvalidoException("Categoria inválida: " + categoria);

        String login = validarAcesso(r.login(), r.senha());
        if (ongs.existsByCnpj(cnpj)) throw new RegraNegocioException("Já existe uma ONG cadastrada com este CNPJ.");

        var ong = ongs.save(OngParceira.builder().nome(nome).cnpj(cnpj)
                .categoriasAceitas(new ArrayList<>(escolhidas)).ativa(true).build());
        contas.save(ContaAcesso.builder().login(login).senhaHash(encoder.encode(r.senha())).ong(ong).build());
    }

    // ------------------------------------------------------------------ apoio

    private String validarAcesso(String login, String senha) {
        String l = login == null ? "" : login.trim();
        if (!LOGIN.matcher(l).matches())
            throw new ParametroInvalidoException(
                    "Login inválido: use de 3 a 50 caracteres, só letras minúsculas, números, ponto, hífen ou sublinhado.");
        if (senha == null || senha.length() < 8 || senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ParametroInvalidoException("A senha deve ter de 8 a 72 caracteres.");
        if (contas.existsByLogin(l)) throw new RegraNegocioException("Este login já está em uso. Escolha outro.");
        return l;
    }

    private static String textoObrigatorio(String valor, String campo, int max) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty()) throw new ParametroInvalidoException(campo + " é obrigatório.");
        if (v.length() > max) throw new ParametroInvalidoException(campo + " é muito longo.");
        return v;
    }

    private static void exigirConvite(String esperado, String informado, String tipo) {
        if (esperado == null || esperado.isBlank())
            throw new RegraNegocioException("O cadastro de " + tipo + " não está habilitado neste ambiente.");
        byte[] a = esperado.trim().getBytes(StandardCharsets.UTF_8);
        byte[] b = (informado == null ? "" : informado.trim()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) throw new ParametroInvalidoException("Código de convite inválido.");
    }

    /** Pedido de demonstracao: ate 3 produtos do catalogo, com prazo de 30 dias para devolver. */
    private void criarPedidoDemo(Usuario usuario) {
        var catalogo = produtos.findAll(PageRequest.of(0, 3, Sort.by("id"))).getContent();
        if (catalogo.isEmpty()) return;
        var agora = LocalDateTime.now(clock);
        var pedido = Pedido.builder()
                .numeroPedido(String.format("DEMO-%06d", usuario.getId()))
                .dataCompra(agora).usuario(usuario).build();
        for (int i = 0; i < catalogo.size(); i++) {
            pedido.getItens().add(ItemComprado.builder().produto(catalogo.get(i)).pedido(pedido).quantidade(1)
                    .valorPago(PRECOS_DEMO[i]).dataLimiteDevolucao(agora.toLocalDate().plusDays(30)).build());
        }
        pedidos.save(pedido);
    }
}
