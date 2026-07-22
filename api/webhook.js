import { GoogleGenerativeAI } from "@google/generative-ai";

export default async function handler(req, res) {
  // 1. A porta de entrada: só aceita requisição POST (que é o formato do Atalhos)
  if (req.method !== 'POST') {
    return res.status(405).json({ erro: 'Método não permitido' });
  }

  try {
    // 2. Pega a frase que o seu iPhone mandou
    const fraseDoIphone = req.body.texto_usuario;

    if (!fraseDoIphone) {
      return res.status(400).json({ erro: 'Nenhum texto foi enviado' });
    }

    // 3. Acorda a Aurora (A sua chave da API vai ficar segura dentro da Vercel depois)
    const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    // 4. Manda a frase para ela processar
    const resultado = await model.generateContent(fraseDoIphone);
    const respostaDaAurora = resultado.response.text();

    // 5. Devolve o resultado com sucesso
    return res.status(200).json({
      sucesso: true,
      mensagem: "A Aurora recebeu sua mensagem!",
      resposta: respostaDaAurora
    });

  } catch (erro) {
    console.error("Erro interno:", erro);
    return res.status(500).json({ erro: 'Falha na comunicação com a IA' });
  }
}
