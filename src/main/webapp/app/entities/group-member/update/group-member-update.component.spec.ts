import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IGroup } from 'app/entities/group/group.model';
import { GroupService } from 'app/entities/group/service/group.service';
import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IGroupMember } from '../group-member.model';
import { GroupMemberService } from '../service/group-member.service';
import { GroupMemberFormService } from './group-member-form.service';

import { GroupMemberUpdateComponent } from './group-member-update.component';

describe('GroupMember Management Update Component', () => {
  let comp: GroupMemberUpdateComponent;
  let fixture: ComponentFixture<GroupMemberUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let groupMemberFormService: GroupMemberFormService;
  let groupMemberService: GroupMemberService;
  let groupService: GroupService;
  let userService: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GroupMemberUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(GroupMemberUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(GroupMemberUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    groupMemberFormService = TestBed.inject(GroupMemberFormService);
    groupMemberService = TestBed.inject(GroupMemberService);
    groupService = TestBed.inject(GroupService);
    userService = TestBed.inject(UserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Group query and add missing value', () => {
      const groupMember: IGroupMember = { id: 23461 };
      const group: IGroup = { id: 25136 };
      groupMember.group = group;

      const groupCollection: IGroup[] = [{ id: 25136 }];
      jest.spyOn(groupService, 'query').mockReturnValue(of(new HttpResponse({ body: groupCollection })));
      const additionalGroups = [group];
      const expectedCollection: IGroup[] = [...additionalGroups, ...groupCollection];
      jest.spyOn(groupService, 'addGroupToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupMember });
      comp.ngOnInit();

      expect(groupService.query).toHaveBeenCalled();
      expect(groupService.addGroupToCollectionIfMissing).toHaveBeenCalledWith(
        groupCollection,
        ...additionalGroups.map(expect.objectContaining),
      );
      expect(comp.groupsSharedCollection).toEqual(expectedCollection);
    });

    it('should call User query and add missing value', () => {
      const groupMember: IGroupMember = { id: 23461 };
      const user: IUser = { id: 3944 };
      groupMember.user = user;

      const userCollection: IUser[] = [{ id: 3944 }];
      jest.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [user];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      jest.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ groupMember });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(expect.objectContaining),
      );
      expect(comp.usersSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const groupMember: IGroupMember = { id: 23461 };
      const group: IGroup = { id: 25136 };
      groupMember.group = group;
      const user: IUser = { id: 3944 };
      groupMember.user = user;

      activatedRoute.data = of({ groupMember });
      comp.ngOnInit();

      expect(comp.groupsSharedCollection).toContainEqual(group);
      expect(comp.usersSharedCollection).toContainEqual(user);
      expect(comp.groupMember).toEqual(groupMember);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupMember>>();
      const groupMember = { id: 25116 };
      jest.spyOn(groupMemberFormService, 'getGroupMember').mockReturnValue(groupMember);
      jest.spyOn(groupMemberService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupMember });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupMember }));
      saveSubject.complete();

      // THEN
      expect(groupMemberFormService.getGroupMember).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(groupMemberService.update).toHaveBeenCalledWith(expect.objectContaining(groupMember));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupMember>>();
      const groupMember = { id: 25116 };
      jest.spyOn(groupMemberFormService, 'getGroupMember').mockReturnValue({ id: null });
      jest.spyOn(groupMemberService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupMember: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: groupMember }));
      saveSubject.complete();

      // THEN
      expect(groupMemberFormService.getGroupMember).toHaveBeenCalled();
      expect(groupMemberService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IGroupMember>>();
      const groupMember = { id: 25116 };
      jest.spyOn(groupMemberService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ groupMember });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(groupMemberService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareGroup', () => {
      it('should forward to groupService', () => {
        const entity = { id: 25136 };
        const entity2 = { id: 19049 };
        jest.spyOn(groupService, 'compareGroup');
        comp.compareGroup(entity, entity2);
        expect(groupService.compareGroup).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        jest.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
