package br.com.alura.technews.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.alura.technews.asynctask.BaseAsyncTask
import br.com.alura.technews.database.dao.NoticiaDAO
import br.com.alura.technews.model.Noticia
import br.com.alura.technews.retrofit.webclient.NoticiaWebClient

class NoticiaRepository(
    private val dao: NoticiaDAO,
    private val webclient: NoticiaWebClient = NoticiaWebClient()
) {
    private val liveData = MutableLiveData<Resource<List<Noticia>?>>()

    fun buscaTodos() : LiveData<Resource<List<Noticia>?>> {
        buscaInterno(quandoSucesso = {listaInterna ->
            liveData.value = Resource(dado = listaInterna)
        })

        buscaNaApi(quandoSucesso = {listaApi ->
            liveData.value = Resource(dado = listaApi)
        },
            quandoFalha = {erro ->
            val resourceAtual = liveData.value
            val resourceCriado = criaResourceDeFalha<List<Noticia>?>(resourceAtual, erro)

            liveData.value = resourceCriado
        })
        return liveData
    }



    fun salva(noticia: Noticia): LiveData<Resource<Void?>> {
        val liveData = MutableLiveData<Resource<Void?>>()
        salvaNaApi(noticia, quandoSucesso = {
            liveData.value = Resource(null)
        }, quandoFalha = {erro ->
            liveData.value = Resource(null, erro)
        })
        return liveData
    }

    fun remove(
        noticia: Noticia,
        quandoSucesso: () -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        removeNaApi(noticia, quandoSucesso, quandoFalha)
    }

    fun edita(
        noticia: Noticia,
        quandoSucesso: (noticiaEditada: Noticia) -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        editaNaApi(noticia, quandoSucesso, quandoFalha)
    }

    fun buscaPorId(
        noticiaId: Long,
        quandoSucesso: (noticiaEncontrada: Noticia?) -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.buscaPorId(noticiaId)
        }, quandoFinaliza = quandoSucesso)
            .execute()
    }

    private fun buscaNaApi(
        quandoSucesso: (List<Noticia>) -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.buscaTodas(
            quandoSucesso = { noticiasNovas ->
                noticiasNovas?.let {
                    salvaInterno(noticiasNovas, quandoSucesso)
                }
            }, quandoFalha = quandoFalha
        )
    }

    private fun buscaInterno(quandoSucesso: (List<Noticia>) -> Unit) {
        BaseAsyncTask(quandoExecuta = {
            dao.buscaTodos()
        }, quandoFinaliza = quandoSucesso)
            .execute()
    }


    private fun salvaNaApi(
        noticia: Noticia,
        quandoSucesso: (noticiaNova: Noticia) -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.salva(
            noticia,
            quandoSucesso = {
                it?.let { noticiaSalva ->
                    salvaInterno(noticiaSalva, quandoSucesso)
                }
            }, quandoFalha = quandoFalha
        )
    }

    private fun salvaInterno(
        noticias: List<Noticia>,
        quandoSucesso: (noticiasNovas: List<Noticia>) -> Unit
    ) {
        BaseAsyncTask(
            quandoExecuta = {
                dao.salva(noticias)
                dao.buscaTodos()
            }, quandoFinaliza = quandoSucesso
        ).execute()
    }

    private fun salvaInterno(
        noticia: Noticia,
        quandoSucesso: (noticiaNova: Noticia) -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.salva(noticia)
            dao.buscaPorId(noticia.id)
        }, quandoFinaliza = { noticiaEncontrada ->
            noticiaEncontrada?.let {
                quandoSucesso(it)
            }
        }).execute()

    }

    private fun removeNaApi(
        noticia: Noticia,
        quandoSucesso: () -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.remove(
            noticia.id,
            quandoSucesso = {
                removeInterno(noticia, quandoSucesso)
            },
            quandoFalha = quandoFalha
        )
    }


    private fun removeInterno(
        noticia: Noticia,
        quandoSucesso: () -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.remove(noticia)
        }, quandoFinaliza = {
            quandoSucesso()
        }).execute()
    }

    private fun editaNaApi(
        noticia: Noticia,
        quandoSucesso: (noticiaEditada: Noticia) -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.edita(
            noticia.id, noticia,
            quandoSucesso = { noticiaEditada ->
                noticiaEditada?.let {
                    salvaInterno(noticiaEditada, quandoSucesso)
                }
            }, quandoFalha = quandoFalha
        )
    }

}
