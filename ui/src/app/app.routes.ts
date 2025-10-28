import { Routes } from '@angular/router';
import { LatestComponent } from './pages/latest/latest.component';
import { ArticleDetailComponent } from './pages/article-detail/article-detail.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { MyTagsComponent } from './pages/my-tags/my-tags.component';
import { UploadComponent } from './pages/upload/upload.component';
import { RecommendComponent } from './pages/recommend/recommend.component';
import { KnowledgeGraphComponent } from './pages/knowledge-graph/knowledge-graph.component';

export const routes: Routes = [
  { path: '', redirectTo: 'latest', pathMatch: 'full' },
  { path: 'latest', component: LatestComponent },
  { path: 'recommend', component: RecommendComponent },
  { path: 'articles/:id', component: ArticleDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'my-tags', component: MyTagsComponent },
  { path: 'upload', component: UploadComponent },
  { path: 'knowledge-graph', component: KnowledgeGraphComponent },
  { path: '**', redirectTo: 'latest' },
];
