package com.mhss.app.data.di

import com.mhss.app.domain.di.AiDomainModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.core.qualifier.named
import com.mhss.app.data.OpenaiApi
import com.mhss.app.domain.repository.AiApi

@Module
@ComponentScan("com.mhss.app.data")
internal class AiDataModule

val aiApiImplModule = module {
    single<AiApi>(named("openaiApi")) { OpenaiApi(get(), get()) }

}

val aiDataModule = module {
    includes(AiDataModule().module, AiDomainModule().module, aiApiImplModule)
}
