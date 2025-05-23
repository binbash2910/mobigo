import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import GroupMemberResolve from './route/group-member-routing-resolve.service';

const groupMemberRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/group-member.component').then(m => m.GroupMemberComponent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/group-member-detail.component').then(m => m.GroupMemberDetailComponent),
    resolve: {
      groupMember: GroupMemberResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/group-member-update.component').then(m => m.GroupMemberUpdateComponent),
    resolve: {
      groupMember: GroupMemberResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/group-member-update.component').then(m => m.GroupMemberUpdateComponent),
    resolve: {
      groupMember: GroupMemberResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default groupMemberRoute;
